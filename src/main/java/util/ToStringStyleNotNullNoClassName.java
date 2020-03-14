package util;

import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * A {@link ToStringStyle} that seems to be like {@link ToStringStyle#NO_CLASS_NAME_STYLE} but avoids printing of null attributes.
 */
public class ToStringStyleNotNullNoClassName extends ToStringStyle {
    private static final long serialVersionUID = 1L;
    public static final ToStringStyle INSTANCE = new ToStringStyleNotNullNoClassName();

    /**
     * <p>Constructor.</p>
     * <p>Use the static constant rather than instantiating.</p>
     */
    ToStringStyleNotNullNoClassName() {
        super();
        this.setUseClassName(false);
        this.setUseIdentityHashCode(false);
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
