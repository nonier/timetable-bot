package ru.tvgu.telegrambot.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.tvgu.telegrambot.entity.Class;
import ru.tvgu.telegrambot.entity.StudyGroup;
import ru.tvgu.telegrambot.entity.WeekType;
import ru.tvgu.telegrambot.entity.TelegramUser;
import ru.tvgu.telegrambot.repository.SubjectRepository;
import ru.tvgu.telegrambot.service.SendMessageService;
import ru.tvgu.telegrambot.service.TimetableService;
import ru.tvgu.telegrambot.utils.DateUtils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class TimetableServiceImpl implements TimetableService {

    public static final String TIMETABLE_FOR_TODAY = "Расписание на сегодня";
    public static final String TIMETABLE_FOR_WEEK = "Расписание на неделю";

    private final SubjectRepository subjectRepository;
    private final SendMessageService sendMessageService;

    @Autowired
    public TimetableServiceImpl(SubjectRepository subjectRepository, SendMessageService sendMessageService) {
        this.subjectRepository = subjectRepository;
        this.sendMessageService = sendMessageService;
    }


    @Override
    public String getTimeTableForToday(StudyGroup studyGroup) {
        LocalDate now = LocalDate.now();
        WeekType period = DateUtils.getWeekPeriodByDate(now);
        return subjectRepository.findAllByStudyGroup(studyGroup)
                .stream()
                .flatMap(subject -> subject.getClasses().stream())
                .filter(cls -> Objects.equals(period, cls.getWeekType())
                        || WeekType.EVERY.equals(cls.getWeekType()))
                .filter(cls -> Objects.equals(now.getDayOfWeek(), cls.getDayOfWeek()))
                .map(this::parseTodayClass)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public String getTimeTableForWeek(StudyGroup studyGroup) {
        LocalDate now = LocalDate.now();
        WeekType period = DateUtils.getWeekPeriodByDate(now);
        return subjectRepository.findAllByStudyGroup(studyGroup)
                .stream()
                .flatMap(subject -> subject.getClasses().stream())
                .filter(cls -> Objects.equals(period, cls.getWeekType())
                        || WeekType.EVERY.equals(cls.getWeekType()))
                .map(this::parseWeekClass)
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
    }

    @Override
    public void getTimeTable(TelegramUser telegramUser, Update update) {
        Long chatId = update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId()
                : update.getMessage().getChatId();
        if (update.hasCallbackQuery()) {
            if (TIMETABLE_FOR_TODAY.equals(update.getCallbackQuery().getData())) {
                String timeTableForToday = getTimeTableForToday(telegramUser.getStudyGroup());
                sendMessageService.sendMessage(chatId, timeTableForToday.isEmpty() ? "Нет данных для данной группы" : timeTableForToday,
                        Collections.emptyList());
            }
            if (TIMETABLE_FOR_WEEK.equals(update.getCallbackQuery().getData())) {
                String timeTableForWeek = getTimeTableForWeek(telegramUser.getStudyGroup());
                sendMessageService.sendMessage(chatId, timeTableForWeek.isEmpty() ? "Нет данных для данной группы" : timeTableForWeek,
                        Collections.emptyList());
            }
        } else {
            sendMessageService.sendMessage(chatId, "Выбери расписание",
                    List.of(TIMETABLE_FOR_TODAY, TIMETABLE_FOR_WEEK));
        }
    }

    private String parseTodayClass(Class cl) {
        return cl.getSubject().getName() +
                System.lineSeparator() +
                cl.getTime() +
                System.lineSeparator() +
                cl.getSubject().getTeacher() +
                System.lineSeparator() +
                cl.getAudienceNumber() +
                System.lineSeparator();
    }

    private String parseWeekClass(Class cl) {
        return cl.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")) +
                System.lineSeparator() +
                cl.getSubject().getName() +
                System.lineSeparator() +
                cl.getTime() +
                System.lineSeparator() +
                cl.getSubject().getTeacher() +
                System.lineSeparator() +
                cl.getAudienceNumber() +
                System.lineSeparator();
    }
}