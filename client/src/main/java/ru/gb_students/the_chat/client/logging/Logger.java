package ru.gb_students.the_chat.client.logging;

import com.sun.source.tree.Tree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
    private String logTemplatePath = "client/history_%s.log";
    private Path logPath;
    private DateFormat dateFormat;
    private boolean firstInsert = true;
    private boolean blockAddition = false;


    public Logger(String login, DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        logTemplatePath = String.format(logTemplatePath, login);
        logPath = Paths.get(logTemplatePath);
        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }
    }

    public Timestamp getLogTimestamp() {
        File fileLog = new File(logPath.toString());
        Timestamp lastTimeStamp = new Timestamp(0);
        if (fileLog.exists() && fileLog.length() != 0) {
            String lastLine = readLinesFromLogEnd(1);
            Pattern timePattern = Pattern.compile("(\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] )");
            Matcher matchTimeInLine = timePattern.matcher(lastLine);
            if (matchTimeInLine.find()) {
                try {
                    Date parsedDate = dateFormat.parse(matchTimeInLine.group(1));
                    lastTimeStamp = new Timestamp(parsedDate.getTime());
                } catch (ParseException e) {
                }
            }
        }
        return lastTimeStamp;
    }

    public String getLocalHistory(int countMessages) {
        return readLinesFromLogEnd(countMessages);
    }

    private String readLinesFromLogEnd(int linesCount) {
        try (RandomAccessFile fileLogRAF = new RandomAccessFile(logPath.toString(), "r")) {
            fileLogRAF.seek(fileLogRAF.length());
            ArrayList<Byte> bytesArray = new ArrayList<>();
            long offset = 0;
            byte c = -1;
            for (int i = 0; i < linesCount; i++) {
                boolean eol = false;
                while (!eol) {
                    offset++;
                    if (offset > fileLogRAF.length()) {
                        break;
                    }
                    fileLogRAF.seek(fileLogRAF.length() - offset);
                    switch (c = fileLogRAF.readByte()) {
                        case -1:
                        case '\n':
                            bytesArray.add(c);
                            eol = true;
                            break;
                        case '\r':
                            eol = true;
                            bytesArray.add(c);
                            long cur = fileLogRAF.getFilePointer();
                            if ((fileLogRAF.read()) != '\n') {
                                fileLogRAF.seek(cur);
                            }
                            break;
                        default:
                            bytesArray.add(c);
                            break;
                    }
                }
            }
            Collections.reverse(bytesArray);
            String result = convertArrayListOfBytesToPrimitiveArray(bytesArray);
            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void putLog(String logRecord) {
        if (blockAddition) {
            return;
        }
        //we check it only once because with high intensity it's heavy for slow filesystem to check emptyness every time
        putLogIgnoreBlock(logRecord);
    }

    public void putLogIgnoreBlock(String logRecord) {
        String logToWrite;
        if (firstInsert && logFileEmpty()) {
            firstInsert = false;
            logToWrite = logRecord.replaceAll("\\s+$", "");;
        } else {
            logToWrite = System.lineSeparator() + logRecord.replaceAll("\\s+$", "");;
        }

        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(logPath.toString(), true))) {
            outputStream.write(logToWrite.getBytes());
        } catch (IOException e){
            new RuntimeException(e);
        }
    }

    private boolean logFileEmpty() {

        if (!Files.exists(logPath)) {
            try {
                Files.createFile(logPath);
            } catch (IOException e) {
                new RuntimeException(e);
            }
            return true;
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(logPath.toString()))) {
                return br.readLine() == null;
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }

        return true;
    }

    private String convertArrayListOfBytesToPrimitiveArray(ArrayList<Byte> list) {
        Byte[] arrayOfBytesWrapper = list.toArray(new Byte[list.size()]);
        byte[] arrayOfBytes = new byte[arrayOfBytesWrapper.length];
        for (int i = 0; i < arrayOfBytesWrapper.length; i++) {
            arrayOfBytes[i] = (byte) arrayOfBytesWrapper[i];
        }
        return new String(arrayOfBytes, StandardCharsets.UTF_8);
    }

    public void setBlockAddition() {
        blockAddition = true;
    }
    public void removeBlockAddition() {
        blockAddition = false;
    }
}
