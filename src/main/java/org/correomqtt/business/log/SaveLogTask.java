package org.correomqtt.business.log;

import org.correomqtt.business.concurrent.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class SaveLogTask extends Task<Void,Void> {
    private final File file;
    private final String content;

    public SaveLogTask(File file, String content) {
        this.file = file;
        this.content = content;
    }

    @Override
    protected Void execute() throws Exception {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.append(content);
            return null;
        }
    }

    //TODO fail
}
