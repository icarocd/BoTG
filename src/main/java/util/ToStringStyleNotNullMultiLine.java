package util;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A {@link ToStringStyle} that seems to be like {@link ToStringStyle#MULTI_LINE_STYLE} but avoids printing of null attributes.
 */
public class ToStringStyleNotNullMultiLine extends ToStringStyle {
    private static final long serialVersionUID = 1L;
    public static final ToStringStyle INSTANCE = new ToStringStyleNotNullMultiLine();

    /**
     * <p>Constructor.</p>
     * <p>Use the static constant rather than instantiating.</p>
     */
    ToStringStyleNotNullMultiLine() {
        super();
        this.setContentStart("[");
        this.setFieldSeparator(SystemUtils.LINE_SEPARATOR + "  ");
        this.setFieldSeparatorAtStart(true);
        this.setContentEnd(SystemUtils.LINE_SEPARATOR + "]");
    }

    /**
     * <p>Ensure <code>Singleton</code> after serialization.</p>
     * @return the singleton
     */
    private Object readResolve() {
        return INSTANCE;
    }

    @Override
    public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
        if (value != null) {
            appendFieldStart(buffer, fieldName);
            appendInternal(buffer, fieldName, value, isFullDetail(fullDetail));
            appendFieldEnd(buffer, fieldName);
        }
    }
}