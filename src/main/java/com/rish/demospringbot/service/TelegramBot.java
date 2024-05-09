package com.rish.demospringbot.service;

import com.rish.demospringbot.config.BotConfig;
import com.rish.demospringbot.model.User;
import com.rish.demospringbot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {


    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    @Autowired
    private UserRepository userRepository;

    private static final String HELP_TEXT = "This bot is to demonstrate Spring capabilities.\n\n +" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n +" +
            "Type /start to see a welcome message\n\n +" +
            "Type /mydata to see data stored about yourself\n\n +" +
            "Type /deletedata you can delete your data\n\n +" +
            "Type /help to see this message again\n\n +" +
            "Type /settings you can change your preferences";
    private final BotConfig config;


    public TelegramBot(@Value("${bot.token}") String botToken, BotConfig config){
        super(botToken);
        this.config = config;
        List<BotCommand> commandList = new ArrayList<>();
        commandList.add(new BotCommand("/start", "get a welcome message"));
        commandList.add(new BotCommand("/mydata", "get your data stored"));
        commandList.add(new BotCommand("/deletedata", "delete my data"));
        commandList.add(new BotCommand("/help", "info how to use this bot"));
        commandList.add(new BotCommand("/settings", "set your preference"));

        try {
            this.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
        }catch (TelegramApiException e) {
            log.error("Error setting bot`s command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String messages = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messages.contains("/send")) {
                String textSend = EmojiParser.parseToUnicode(messages.substring(messages.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                for (User user: users){
                    sendMessage(user.getChatId(), textSend);
                }
            }
            else {

                switch (messages) {
                    case "/start":

                        registerUser(update.getMessage());

                        startCommand(chatId, update.getMessage().getChat().getFirstName());
                        break;

                    case "/help":
                        sendMessage(chatId, HELP_TEXT);
                        break;

                    case "/register":
                        register(chatId);
                        break;

                    default:
                        sendMessage(chatId, "Sorry!");
                }
            }

        }else if (update.hasCallbackQuery()){
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                String text = "Thank you, you clicked yes!";
                extractedEditMessage(chatId, text, (int) messageId);

            } else if (callbackData.equals(NO_BUTTON)){
                String text = "Thank you, you clicked no!";
                extractedEditMessage(chatId, text, (int) messageId);
            }
        }

    }

    private void extractedEditMessage(long chatId, String text, int messageId) {
        EditMessageText mesText = new EditMessageText();
        mesText.setChatId(String.valueOf(chatId));
        mesText.setText(text);
        mesText.setMessageId(messageId);


        try {
            execute(mesText);
        }catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }


    private void register(Long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Do you want to register?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> inLineRows = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton button_yes = new InlineKeyboardButton();
        button_yes.setText("Yes");
        button_yes.setCallbackData(YES_BUTTON);

        InlineKeyboardButton button_no = new InlineKeyboardButton();
        button_no.setText("No");
        button_no.setCallbackData(NO_BUTTON);

        rowInLine.add(button_yes);
        rowInLine.add(button_no);
        inLineRows.add(rowInLine);

        inlineKeyboardMarkup.setKeyboard(inLineRows);
        msg.setReplyMarkup(inlineKeyboardMarkup);

        extracted(msg);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setUserName(chat.getUserName());
            user.setLastName(chat.getLastName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            log.info("user saved: " + user);
        }
    }

    private void startCommand(long chatId, String name){

        String answer = EmojiParser.parseToUnicode( "Hello, " + name + ", nice to meet you!" + " \uD83D\uDE0A");

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);

        keyboard(sendMessage);

        extracted(sendMessage);
    }

    private void extracted(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        }catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private static void keyboard(SendMessage sendMessage) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("Weather");
        row.add("more details");
        keyboardRows.add(row);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("register");
        row2.add("my data");
        row2.add("delete my data");
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(keyboardMarkup);
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }
}
