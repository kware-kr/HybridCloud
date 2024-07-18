package kware.common.excel;

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
public class DefaultExcelCellStyle {

	private SXSSFWorkbook wb;
	private XSSFCellStyle header;
	private XSSFCellStyle body;
	private XSSFCellStyle amount;
	private Font font;


	// workbook을 받아와서 각각의 cellStyle 객체 생성 및 값 리턴
	public Map<String, XSSFCellStyle> setWb(SXSSFWorkbook wb){
		this.wb = wb;
		font = wb.createFont();
		this.header = (XSSFCellStyle)wb.createCellStyle();
		this.body = (XSSFCellStyle)wb.createCellStyle();
		this.amount = (XSSFCellStyle)wb.createCellStyle();
		// map으로 return
		return this.complete();
	}


	// 공통 스타일
	public XSSFCellStyle commonStyle(XSSFCellStyle cellStyle) {
		cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
		cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		font.setFontHeightInPoints((short) 10);
		font.setFontName("신명조");
		cellStyle.setFont(font);

		return cellStyle;
	}


	// header 스타일
	public XSSFCellStyle headerStyle() {
		header = this.commonStyle(this.header);
		header.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 192, (byte) 192, (byte) 192}, null));
		header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return header;
	}

	// contents 스타일
	public XSSFCellStyle bodyStyle() {
		return this.commonStyle(this.body);
	}

	// amount 스타일
	public XSSFCellStyle bodyAmountStyle() {
		amount = this.commonStyle(this.amount);
		amount.setAlignment(HorizontalAlignment.RIGHT);                                  // 해당 값이 AMOUNT type이라면 우측정렬
        amount.setDataFormat(HSSFDataFormat.getBuiltinFormat("#,##0"));
		return amount;
	}
	// 만들어진 객체를 Map에 담아 리턴
	public Map<String, XSSFCellStyle> complete() {
		Map<String, XSSFCellStyle> map = new HashMap<>();
		map.put("headerStyle", this.headerStyle());
		map.put("bodyStyle", this.bodyStyle());
		map.put("amountStyle", this.bodyAmountStyle());
		return map;
	}

}
