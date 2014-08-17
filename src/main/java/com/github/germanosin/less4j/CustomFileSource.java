package com.github.germanosin.less4j;

import com.github.sommeri.less4j.LessSource;
import com.github.sommeri.less4j.utils.URIUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: germanosin
 * Date: 17.08.14
 * Time: 11:33
 * To change this template use File | Settings | File Templates.
 */
public class CustomFileSource extends LessSource.AbstractHierarchicalSource {

    private List<File> paths;
    private File inputFile;
    private String charsetName;

    public CustomFileSource(File inputFile, List<File> paths) {
       this(inputFile, null, paths);
    }

    public CustomFileSource(File inputFile, String charsetName,List<File> paths) {
        this.inputFile = inputFile;
        this.charsetName = charsetName;
        this.paths = paths;
    }

    public CustomFileSource(CustomFileSource parent, String filename) {
        this(parent, filename, null);
    }

    public CustomFileSource(CustomFileSource parent, String filename, String charsetName) {
        super(parent);
        this.paths = parent.paths;
        this.inputFile = findFile(parent, filename);
        this.charsetName = charsetName;
        parent.addImportedSource(this);
    }

    @Override
    public CustomFileSource relativeSource(String filename) {
        return new CustomFileSource(this, filename);
    }

    private File findFile(CustomFileSource parent, String filename) {
        File inputFile = new File(parent.getInputFile().getParentFile(), filename);
        if (!inputFile.exists() && paths!=null)
            for (File parentFile : paths) {
                inputFile = new File(parentFile, filename);
                if (inputFile.exists()) break;
            }
        return inputFile;
    }

    @Override
    public URI getURI() {
        try {
            String path = inputFile.toString();
            path = URIUtils.convertPlatformSeparatorToUri(path);
            return new URI(path);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public String getName() {
        return inputFile.getName();
    }

    @Override
    public String getContent() throws FileNotFound, CannotReadFile {
        try {
            Reader input;
            if (charsetName != null) {
                input = new InputStreamReader(new FileInputStream(inputFile), charsetName);
            } else {
                input = new FileReader(inputFile);
            }
            try {
                String content = IOUtils.toString(input).replace("\r\n", "\n");
                setLastModified(inputFile.lastModified());
                return content;
            } finally {
                input.close();
            }
        } catch (FileNotFoundException ex) {
            throw new FileNotFound();
        } catch (IOException ex) {
            throw new CannotReadFile();
        }
    }

    @Override
    public byte[] getBytes() throws FileNotFound, CannotReadFile {
        try {
            byte[] content = FileUtils.readFileToByteArray(inputFile);
            setLastModified(inputFile.lastModified());
            return content;
        } catch (FileNotFoundException ex) {
            throw new FileNotFound();
        } catch (IOException ex) {
            throw new CannotReadFile();
        }
    }


    public File getInputFile() {
        return inputFile;
    }

    public String toString() {
        return inputFile.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        File canonicalInputFile = getCanonicalFile();
        String filePath = canonicalInputFile.getAbsolutePath();
        result = prime * result + ((canonicalInputFile == null) ? 0 : canonicalInputFile.hashCode());
        result = prime * result + ((charsetName == null) ? 0 : charsetName.hashCode());
        return result;
    }

    private File getCanonicalFile() {
        return getCanonicalFile(inputFile);
    }

    private File getCanonicalFile(File inputFile) {
        try {
            return inputFile.getCanonicalFile();
        } catch (IOException e) {
            return inputFile.getAbsoluteFile();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CustomFileSource other = (CustomFileSource) obj;
        File absoluteInputFile = getCanonicalFile();
        File otherAbsoluteInputFile = getCanonicalFile(other.inputFile);
        if (absoluteInputFile == null) {
            if (other.inputFile != null)
                return false;
        } else if (!absoluteInputFile.equals(otherAbsoluteInputFile))
            return false;
        if (charsetName == null) {
            if (other.charsetName != null)
                return false;
        } else if (!charsetName.equals(other.charsetName))
            return false;
        return true;
    }
}
