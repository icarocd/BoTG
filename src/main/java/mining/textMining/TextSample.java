package mining.textMining;
import java.util.Set;
import mining.Sample;
import util.StringUtils;

public class TextSample extends Sample {

	private String title;
	private String body;
    private String[] tags;

	public TextSample(long id, Set<String> labels) {
        super(id, labels);
    }
	public TextSample(long id, Set<String> labels, String title, String body, String[] tags) {
        super(id, labels);
        this.title = title;
        this.body = body;
        this.tags = tags;
    }

    public TextSample(long id) {
	    this(id, null);
	}

    public String getTitle() {
        return title;
    }

    public boolean isTitleEmpty() {
        return StringUtils.isBlank(title);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

	public boolean isBodyEmpty() {
		return StringUtils.isBlank(body);
	}

	public void setBody(String body) {
        this.body = body;
    }

	public String[] getTags() {
		return tags;
	}
}