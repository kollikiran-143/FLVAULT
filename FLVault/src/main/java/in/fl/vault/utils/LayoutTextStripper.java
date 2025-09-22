package in.fl.vault.utils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class LayoutTextStripper extends PDFTextStripper{	
	private float fixedCharWidth = 3;
	private boolean endsWithWS = true;
    private boolean needsWS = false;
    private int chars = 0;

    private PDRectangle cropBox = null;
    private float pageLeft = 0;
    
    public LayoutTextStripper() throws IOException
    {
        super();
    }

    @Override
    protected void startPage(PDPage page) throws IOException
    {
        super.startPage(page);
        cropBox = page.getCropBox();
        pageLeft = cropBox.getLowerLeftX();
        beginLine();
    }

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException
    {
        float recentEnd = 0;
        for (TextPosition textPosition: textPositions)
        {
            String textHere = textPosition.toString();
            if (textHere.trim().length() == 0)
                continue;

            float start = textPosition.getX();
            boolean spacePresent = endsWithWS | textHere.startsWith(" ");

            if (needsWS | spacePresent | Math.abs(start - recentEnd) > 1)
            {
                int spacesToInsert = insertSpaces(chars, start, needsWS & !spacePresent);

                for (; spacesToInsert > 0; spacesToInsert--)
                {
                    writeString(" ");
                    chars++;
                }
            }

            writeString(textHere);
            chars += textHere.length();

            needsWS = false;
            endsWithWS = textHere.endsWith(" ");
            try{
                recentEnd = getEndX(textPosition);
            }
            catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e)
            {
                throw new IOException("Failure retrieving endX of TextPosition", e);
            }
        }
    }

    @Override
    protected void writeLineSeparator() throws IOException
    {
        super.writeLineSeparator();
        beginLine();
    }

    @Override
    protected void writeWordSeparator() throws IOException
    {
        needsWS = true;
    }

    private void beginLine()
    {
        endsWithWS = true;
        needsWS = false;
        chars = 0;
    }

    private int insertSpaces(int charsInLineAlready, float chunkStart, boolean spaceRequired)
    {
        int indexNow = charsInLineAlready;
        int indexToBe = (int)((chunkStart - pageLeft) / fixedCharWidth);
        int spacesToInsert = indexToBe - indexNow;
        if (spacesToInsert < 1 && spaceRequired)
            spacesToInsert = 1;

        return spacesToInsert;
    }

    private float getEndX(TextPosition textPosition) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException
    {
        Field field = textPosition.getClass().getDeclaredField("endX");
        field.setAccessible(true);
        return field.getFloat(textPosition);
    }

	
    public float getFixedCharWidth() {
		return fixedCharWidth;
	}

	public void setFixedCharWidth(float fixedCharWidth) {
		this.fixedCharWidth = fixedCharWidth;
	}

	public boolean isEndsWithWS() {
		return endsWithWS;
	}

	public void setEndsWithWS(boolean endsWithWS) {
		this.endsWithWS = endsWithWS;
	}

	public boolean isNeedsWS() {
		return needsWS;
	}

	public void setNeedsWS(boolean needsWS) {
		this.needsWS = needsWS;
	}

	public int getChars() {
		return chars;
	}

	public void setChars(int chars) {
		this.chars = chars;
	}

	public PDRectangle getCropBox() {
		return cropBox;
	}

	public void setCropBox(PDRectangle cropBox) {
		this.cropBox = cropBox;
	}

	public float getPageLeft() {
		return pageLeft;
	}

	public void setPageLeft(float pageLeft) {
		this.pageLeft = pageLeft;
	}
    
}