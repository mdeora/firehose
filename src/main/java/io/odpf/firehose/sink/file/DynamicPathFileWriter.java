package io.odpf.firehose.sink.file;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DynamicPathFileWriter implements FileWriter {

    private Map<PathBuilder, FileWriter> fileWriterMap;
    private FileWriterFactory writerFactory;
    private PathFactory pathFactory;
    private PathBuilder path;

    public DynamicPathFileWriter(PathFactory pathFactory, FileWriterFactory writerFactory) {
        this.pathFactory = pathFactory;
        this.writerFactory = writerFactory;
        this.fileWriterMap = new HashMap<>();
    }

    @Override
    public void open(PathBuilder path) throws IOException {
        this.path = path;
    }

    @Override
    public void write(Record record) throws IOException {
        PathBuilder newPath = createPath(record);

        FileWriter delegateWriter = fileWriterMap.get(newPath);
        if (delegateWriter == null) {
            delegateWriter = writerFactory.createRotatingFileWriter();
            delegateWriter.open(newPath);
            fileWriterMap.put(newPath, delegateWriter);
        }

        delegateWriter.write(record);
    }

    private PathBuilder createPath(Record record) {
        Path partitionedPath = pathFactory.create(record);

        PathBuilder currentPath = path.copy();
        Path currentDir = currentPath.getDir();
        Path partitionedDir = currentDir.resolve(partitionedPath);

        return currentPath.setDir(partitionedDir);
    }

    /**
     *
     * @return always return -1 because the this do not point to single to specific delegate writer
     */
    @Override
    public long getDataSize() {
        return -1;
    }

    @Override
    public void close() throws IOException {
        for (FileWriter writer : this.fileWriterMap.values()) {
            writer.close();
        }
        fileWriterMap.clear();
    }
}
