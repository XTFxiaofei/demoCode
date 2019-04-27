package com.example.demo.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;



public class WmParser {
    //	private static Log log = LogFactory.getLog(WmParser.class);
    public static WmParser wmParser;
    private static String CHARSET = "ISO-8859-1";
    static {
        try {
//			log.debug("Instantiating WmParser....");
            wmParser = new WmParser();
            //敏感词库
            FileInputStream in = new FileInputStream("sensitive.txt");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in));
            String line = null;
            while ((line = reader.readLine()) != null) {
//				wmParser.addFilterKeyWord(new String(line.getBytes(), "ISO-8859-1"), 1);
                wmParser.addFilterKeyWord(line, 1);
//				String[] badWords = line.split("!");
//				if (badWords.length == 0){
//					continue;
//				}
//				if (badWords[0].equals("")){
//					continue;
//				}
//				try {
//					wmParser.addFilterKeyWord(badWords[0], Integer.valueOf(badWords[1]));
//				} catch (NumberFormatException e) {
//					log.error("NumberFormatException in Instantiating WmParser's badWords level:" + e);
//					wmParser.addFilterKeyWord(badWords[0], Integer.valueOf(1));
//				}
            }
            reader.close();
            in.close();
        } catch (Exception e) {
//			log.error("Exception in Instantiating WmParser:" + e);
            e.printStackTrace();
        }
    }
    protected WmParser(){

    }
    public static WmParser getInstance(){
        return wmParser;
    }

    private boolean initFlag = false;
    private UnionPatternSet unionPatternSet = new UnionPatternSet();
    private int maxIndex = (int) java.lang.Math.pow(2, 16);
    private int shiftTable[] = new int[maxIndex];
    public Vector<AtomicPattern> hashTable[] = new Vector[maxIndex];
    private UnionPatternSet tmpUnionPatternSet = new UnionPatternSet();

    public static void main(String args[]) {
        try {
            WmParser filterEngine = WmParser.getInstance();
            Vector<Integer> levelSet = new Vector<Integer>();
            String str = "你好 cao你大爷毛民航局FUCK5l玩引导员";
            SimpleDateFormat sf = new SimpleDateFormat("HH:mm:ss.SSS");
            System.out.println("文本长度：" + str.length());
            System.out.println("敏感词汇总数:" + filterEngine.tmpUnionPatternSet.getSet().size());
            Date start = new Date(System.currentTimeMillis());
            System.out.println("过滤开始：" + sf.format(start));

            System.out.println(str);
            System.out.println(filterEngine.parse(new String(str.getBytes(), "UTF-8"), levelSet));

            Date end = new Date(System.currentTimeMillis());
            System.out.println("过滤完毕：" + sf.format(end));
            System.out.println("文本中出现敏感词个数：" + levelSet.size());
            System.out.println("耗时：" + (end.getTime() - start.getTime()) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public boolean addFilterKeyWord(String keyWord, int level) {
        if (initFlag == true)
            return false;
        UnionPattern unionPattern = new UnionPattern();
        Pattern pattern = new Pattern(keyWord);
        AtomicPattern atomicPattern = new AtomicPattern(pattern);
        unionPattern.addNewAtomicPattrn(atomicPattern);
        unionPattern.setLevel(level);
        atomicPattern.setBelongUnionPattern(unionPattern);
        tmpUnionPatternSet.addNewUnionPattrn(unionPattern);
        return true;
    }

    public String parse(String content, Vector<Integer> levelSet){
        try {
            if (initFlag == false)
                init();
            Vector<AtomicPattern> aps = new Vector<AtomicPattern>();
            StringBuilder sb = new StringBuilder();
            char checkChar;
            for (int i = 0; i < content.length();) {
                checkChar = content.charAt(i);
                if (shiftTable[checkChar] == 0) {
                    Vector<AtomicPattern> tmpAps = new Vector<AtomicPattern>();
                    Vector<AtomicPattern> destAps = hashTable[checkChar];
                    int match = 0;
                    for (int j = 0; j < destAps.size(); j++) {
                        AtomicPattern ap = destAps.get(j);
                        if (ap.findMatchInString(content.substring(0, i + 1))){
                            String patternStr = ap.getPattern().str;
                            if (match > 0){
                                sb.setLength(sb.length() - patternStr.length());
                            } else {
                                sb.setLength(sb.length() - patternStr.length() + 1);
                            }
                            appendStar(patternStr, sb);
                            tmpAps.add(ap);
                            match++;
                        }
                    }
                    aps.addAll(tmpAps);
                    if (tmpAps.size() <= 0){
                        sb.append(checkChar);
                    }
                    i++;
                } else {
                    if (i + shiftTable[checkChar] <= content.length()){
                        sb.append(content.substring(i, i + shiftTable[checkChar]));
                    } else {
                        sb.append(content.substring(i));
                    }
                    i = i + shiftTable[checkChar];
                }
            }
            parseAtomicPatternSet(aps, levelSet);
            return sb.toString();
        } catch (Exception e) {
//			log.error(e);
            e.printStackTrace();
        }
        return "";
    }

    private static void appendStar(String patternStr, StringBuilder sb){
        for (int c = 0;c < patternStr.length(); c++){
            char ch = patternStr.charAt(c);
            if ((ch >= 0x4e00 && ch <= 0x9FA5) || (ch >= 0xF900 && ch <= 0xFA2D)){
                sb.append("＊");
            } else {
                sb.append("*");
            }
        }
    }


    private void parseAtomicPatternSet(Vector<AtomicPattern> aps,
                                       Vector<Integer> levelSet) {
        while (aps.size() > 0) {
            AtomicPattern ap = aps.get(0);
            UnionPattern up = ap.belongUnionPattern;
            if (up.isIncludeAllAp(aps)) {
                levelSet.add(new Integer(up.getLevel()));
            }
            aps.remove(0);
        }
    }

    // shift table and hash table of initialize
    private void init() {
        initFlag = true;
        for (int i = 0; i < maxIndex; i++)
            hashTable[i] = new Vector<AtomicPattern>();
        shiftTableInit();
        hashTableInit();
    }

    public void clear() {
        tmpUnionPatternSet.clear();
        initFlag = false;
    }

    private void shiftTableInit() {
        for (int i = 0; i < maxIndex; i++)
            shiftTable[i] = 2;
        Vector<UnionPattern> upSet = tmpUnionPatternSet.getSet();
        for (int i = 0; i < upSet.size(); i++) {
            Vector<AtomicPattern> apSet = upSet.get(i).getSet();
            for (int j = 0; j < apSet.size(); j++) {
                AtomicPattern ap = apSet.get(j);
                Pattern pattern = ap.getPattern();
                if (shiftTable[pattern.charAtEnd(1)] != 0)
                    shiftTable[pattern.charAtEnd(1)] = 1;
                if (shiftTable[pattern.charAtEnd(0)] != 0)
                    shiftTable[pattern.charAtEnd(0)] = 0;
            }
        }
    }

    private void hashTableInit() {
        Vector<UnionPattern> upSet = tmpUnionPatternSet.getSet();
        for (int i = 0; i < upSet.size(); i++) {
            Vector<AtomicPattern> apSet = upSet.get(i).getSet();
            for (int j = 0; j < apSet.size(); j++) {
                AtomicPattern ap = apSet.get(j);
                Pattern pattern = ap.getPattern();
                if (pattern.charAtEnd(0) != 0) {
                    hashTable[pattern.charAtEnd(0)].add(ap);
                }
            }
        }
    }
}

class Pattern { // string
    Pattern(String str) {
        this.str = str;
    }

    public char charAtEnd(int index) {
        if (str.length() > index) {
            return str.charAt(str.length() - index - 1);
        } else
            return 0;
    }

    public String str;

    public String getStr() {
        return str;
    };
}

class AtomicPattern {
    //	public boolean findMatchInString(String str) throws Exception {
//		String patStr = new String(this.pattern.str.getBytes("ISO-8859-1"), "UTF-8");
//		str = new String(str.getBytes("ISO-8859-1"), "UTF-8");
//		if (patStr.length() > str.length())
//			return false;
//		int beginIndex = str.lastIndexOf(patStr.charAt(0) + "");
//		if (beginIndex != -1){
//			String eqaulLengthStr = str.substring(beginIndex);
//			if (patStr.equalsIgnoreCase(eqaulLengthStr))
//				return true;
//		}
//		return false;
//	}
    public boolean findMatchInString(String str) {
        if (this.pattern.str.length() > str.length())
            return false;
        int beginIndex = str.lastIndexOf(this.pattern.str.charAt(0) + "");
        if (beginIndex != -1){
            String eqaulLengthStr = str.substring(beginIndex);
            if (this.pattern.str.equalsIgnoreCase(preConvert(eqaulLengthStr)))
                return true;
        }
        return false;
    }
    private String preConvert(String content) {
        String retStr = new String();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (this.isValidChar(ch)) {
                retStr = retStr + ch;
            }
        }
        return retStr;
    }
    private boolean isValidChar(char ch) {
        if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z'))
            return true;
        if ((ch >= 0x4e00 && ch <= 0x9FA5) || (ch >= 0xF900 && ch <= 0xFA2D))
            return true;
        return false;
    }

    AtomicPattern(Pattern pattern) {
        this.pattern = pattern;
    };

    private Pattern pattern;
    public UnionPattern belongUnionPattern;

    public UnionPattern getBelongUnionPattern() {
        return belongUnionPattern;
    }

    public void setBelongUnionPattern(UnionPattern belongUnionPattern) {
        this.belongUnionPattern = belongUnionPattern;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
}

class SameAtomicPatternSet {
    SameAtomicPatternSet() {
        SAPS = new Vector<AtomicPattern>();
    };

    public Vector<AtomicPattern> SAPS;
}

class UnionPattern { // union string
    UnionPattern() {
        this.apSet = new Vector<AtomicPattern>();
    }

    public Vector<AtomicPattern> apSet;

    public void addNewAtomicPattrn(AtomicPattern ap) {
        this.apSet.add(ap);
    }

    public Vector<AtomicPattern> getSet() {
        return apSet;
    }

    public boolean isIncludeAllAp(Vector<AtomicPattern> inAps) {
        if (apSet.size() > inAps.size())
            return false;
        for (int i = 0; i < apSet.size(); i++) {
            AtomicPattern ap = apSet.get(i);
            if (isInAps(ap, inAps) == false)
                return false;
        }
        return true;
    }

    private boolean isInAps(AtomicPattern ap, Vector<AtomicPattern> inAps) {
        for (int i = 0; i < inAps.size(); i++) {
            AtomicPattern destAp = inAps.get(i);
            if (ap.getPattern().str.equalsIgnoreCase(destAp.getPattern().str))
                return true;
        }
        return false;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }

    private int level;
}

class UnionPatternSet { // union string set
    UnionPatternSet() {
        this.unionPatternSet = new Vector<UnionPattern>();
    }

    public void addNewUnionPattrn(UnionPattern up) {
        this.unionPatternSet.add(up);
    }

    public Vector<UnionPattern> unionPatternSet;

    public Vector<UnionPattern> getSet() {
        return unionPatternSet;
    }

    public void clear() {
        unionPatternSet.clear();
    }
}