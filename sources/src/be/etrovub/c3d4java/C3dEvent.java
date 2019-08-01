package be.etrovub.c3d4java;

public class C3dEvent
{
	public C3dEvent(String label, String context){
		this(label,context, -1);
	}
    public C3dEvent(String label, String context, int frame) { /* Current frame */
        this._label = label;
        this._context = context;
        this._description = "";
        this._subject = "";
        this._frame = 0;
        this._iconId = 0;
        this._genericFlag = 0;
    }

    private String _context;

    private String _label;
    
    private String _description;

    private String _subject;

    private int _frame;

    private short _iconId;

    private byte _genericFlag;

	public String getContext() {
		return this._context;
	}

	public void setContext(String context) {
		this._context = context;
	}

	public String getLabel() {
		return this._label;
	}

	public void setLabel(String label) {
		this._label = label;
	}

	public String getDescription() {
		return this._description;
	}

	public void setDescription(String description) {
		this._description = description;
	}

	public String getSubject() {
		return this._subject;
	}

	public void setSubject(String subject) {
		this._subject = subject;
	}

	public int getFrame() {
		return this._frame;
	}

	public void setFrame(int frame) {
		this._frame = frame;
	}

	public short getIconId() {
		return this._iconId;
	}

	public void setIconId(short iconId) {
		this._iconId = iconId;
	}

	public byte getGenericFlag() {
		return this._genericFlag;
	}

	public void setGenericFlag(byte genericFlag) {
		this._genericFlag = genericFlag;
	}
}
