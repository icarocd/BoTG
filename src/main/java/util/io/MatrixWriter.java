package util.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import org.apache.commons.io.IOUtils;

public class MatrixWriter implements AutoCloseable {

    private DecimalFormat decimalFormatter;
    private Writer writer;
    private char valueSeparator;

    public MatrixWriter(File resultFile) {
        this(FileUtils.createWriterToFile(resultFile), null, ',');
    }

    public MatrixWriter(Writer writer, Integer maxDecimalDigits, char valueSeparator) {
        decimalFormatter = FileUtils.getDecimalFormatter(maxDecimalDigits);
        this.writer = writer;
        this.valueSeparator = valueSeparator;
    }

    public MatrixWriter add(Object text) {
        try {
            writer.write(text.toString());
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void separate() {
    	try {
    		writer.write(valueSeparator);
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
	}

    public MatrixWriter separateAndAdd(Object text) {
        separate();
        add(text);
        return this;
    }

	public void newLine() {
        try {
            writer.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addFormattedDecimal(float number) {
        add(decimalFormatter.format(number));
    }
    public void addFormattedDecimal(double number) {
        add(decimalFormatter.format(number));
    }

    public void separateAndAddFormattedDecimal(float number) {
        separateAndAdd(decimalFormatter.format(number));
    }
    public void separateAndAddFormattedDecimal(double number) {
        separateAndAdd(decimalFormatter.format(number));
    }

    public void flush() {
    	try {
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }

    public void close() {
        try {
        	flush();
        } finally {
            IOUtils.closeQuietly(writer);
            writer = null;
        }
    }

	public <T> void print(T[][] matrix) {
        try {
            for (int row = 0; row < matrix.length; row++) {
                int nColumns = matrix[row].length;
                for (int column = 0; column < nColumns; column++) {
                    T value = matrix[row][column];
                    if(value instanceof Float || value instanceof Double){
                        writer.write(decimalFormatter.format(value));
                    }else if(value != null){
                        writer.write(value.toString());
                    }else{
                        writer.write(valueSeparator);
                    }

                    if(column < nColumns - 1){
                        writer.write(valueSeparator);
                    }
                }
                writer.write('\n');
            }
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
