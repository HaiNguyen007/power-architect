package ca.sqlpower.architect.profile;

public class TableProfileResult extends ProfileResult {

    private int rowCount;

    public TableProfileResult(long createStartTime) {
        super(createStartTime);
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    @Override
    public String toString() {
        return "RowCount:" + rowCount +
                "   Run Date:[" + getCreateDate() + "]" +
                "   Time To Create:" + getTimeToCreate() + "ms";
    }
}
