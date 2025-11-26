package com.campus.tracker.model;

public class CAMarks {
    private int id;
    private int subjectId;
    private String type; // "LAB" or "THEORY"

    // Lab fields
    private String lt1;
    private String lt2;

    // Theory fields
    private String t1;
    private String t2;
    private String rt;
    private String rt1;
    private String rt2;
    private String totalBeforeAP;
    private String ap;
    private String mp1;
    private String mp2;

    // Common fields
    private String total;
    private String convTotal;

    // Constructors
    public CAMarks() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLt1() { return lt1; }
    public void setLt1(String lt1) { this.lt1 = lt1; }

    public String getLt2() { return lt2; }
    public void setLt2(String lt2) { this.lt2 = lt2; }

    public String getT1() { return t1; }
    public void setT1(String t1) { this.t1 = t1; }

    public String getT2() { return t2; }
    public void setT2(String t2) { this.t2 = t2; }

    public String getRt() { return rt; }
    public void setRt(String rt) { this.rt = rt; }

    public String getRt1() { return rt1; }
    public void setRt1(String rt1) { this.rt1 = rt1; }

    public String getRt2() { return rt2; }
    public void setRt2(String rt2) { this.rt2 = rt2; }

    public String getTotalBeforeAP() { return totalBeforeAP; }
    public void setTotalBeforeAP(String totalBeforeAP) { this.totalBeforeAP = totalBeforeAP; }

    public String getAp() { return ap; }
    public void setAp(String ap) { this.ap = ap; }

    public String getMp1() { return mp1; }
    public void setMp1(String mp1) { this.mp1 = mp1; }

    public String getMp2() { return mp2; }
    public void setMp2(String mp2) { this.mp2 = mp2; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }

    public String getConvTotal() { return convTotal; }
    public void setConvTotal(String convTotal) { this.convTotal = convTotal; }
}