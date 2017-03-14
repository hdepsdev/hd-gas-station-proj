package com.bhz.eps.util;
//
//import com.bhz.webservice.enums.ResponseEnum;
//import com.bhz.webservice.exception.WebServiceException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public enum DateEnum {
        db_date("yyyy-MM-dd", new SimpleDateFormat("yyyy-MM-dd")),
        db_time("yyyy-MM-dd HH:mm:ss.SSS", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")),
        webservice_date("yyyyMMdd", new SimpleDateFormat("yyyyMMdd")),
        webservice_timeWithMilli("yyyyMMddHHmmssSSS", new SimpleDateFormat("yyyyMMddHHmmssSSS")),
    	webservice_time("yyyyMMddHHmmss", new SimpleDateFormat("yyyyMMddHHmmss"));

        private String title;
        private SimpleDateFormat value;

        DateEnum(String title_,SimpleDateFormat value_) {
            this.value = value_;
            this.title = title_;
        }

        public SimpleDateFormat value() {
            return value;
        }

        public String title() {
            return this.title;
        }
    }

    public final static String FORMAT_PATTERN_NEED_SECOND = "yyyy-MM-dd HH:mm:ss";

    //public final static SimpleDateFormat db_date = new SimpleDateFormat("yyyy-MM-dd");
    //
    //public final static SimpleDateFormat db_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    //
    //public final static SimpleDateFormat webservice_date = new SimpleDateFormat("yyyyMMdd");
    //
    //public final static SimpleDateFormat webservice_time = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    /**
     * 取得系统时间格式
     * @return String yyyyMMddHHmmssSSS
     */
    public static String getSysDateYmdHmss(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String ret = sdf.format(new Date());
        return ret;
    }

    /**
     * 取得系统时间
     *
     * @return Date 系统时间
     */
    public static Date getSysDate() {
        Date sysDate = new Date();
        return sysDate;
    }

    public static String formatByPattern(Date date,String pattern){
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * 取得初始时间格式
     *
     * @return Date 初始时间格式
     */
    public static Date getSysInitDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            return sdf.parse("1900-01-01 00:00:00");
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    public static Date getSysDates(int year) {
		SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_PATTERN_NEED_SECOND);
		Calendar c = Calendar.getInstance();
		if(year==0){
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
		    c.set(Calendar.SECOND, 0);
		}else{
			c.add(Calendar.YEAR, year);
			c.set(Calendar.HOUR_OF_DAY, 23);
			c.set(Calendar.MINUTE, 59);
		    c.set(Calendar.SECOND, 59);
		}
//		String result = sdf.format(c.getTime());
//		System.out.println(result);
        return c.getTime();
	}

    public static String format(Date date,DateEnum dateEnum) {
        return dateEnum.value().format(date);
    }

    public static Date parse(String date, DateEnum dateEnum) {
        try {
            return dateEnum.value().parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args){
        System.out.println(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_date));
        System.out.println(DateUtil.format(new Date(), DateUtil.DateEnum.webservice_time));
    }

}
