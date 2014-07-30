package org.wb.proc.extractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.Format;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

interface Constants{
	String CARRIAGE_RETURN = "\r\n";
	String NEW_LINE = "\n";
	String PATTERN_KEY = "\"[\\w]([\\w\\s-])*?\"";
	String PATTERN_VALUE = "[_A-Z][_A-Z0-9]*[_A-Z0-9]";
	String FORMAT_STRING = "\t\tString %s = %s;\r";
	String REGEX_PATTERN_1 = "[\\s-]+";
	String REGEX_JAVA	= ".java$";
	String TEMP_FILE = "temp.java";
	String PACKAGE_NAME = "package";
	String OPEN_BRACES = "{";
	String CLOSE_BRACES = "}";
	String UNDER_SCORE = "_";
	String REGEX_SLASH = "\"";
	String EMPTY_STRING = "";
	String FMODE_RW = "rw";
}

public class ConstantParser {
	private Scanner scan = null;
	static Pattern keyPattern = Pattern.compile(Constants.PATTERN_KEY);	//double quoted strings
	static Pattern valuePattern = Pattern.compile(Constants.PATTERN_VALUE);//Uppercase constants
	private File sourceFile = null;
	private File destinationFile = null;
	private Map<String, String> constants = null;
	private String interfaceName = null;
	private boolean flag = false;

	public ConstantParser(String destinationFile) {
		super();
		this.setDestinationFile(new File(destinationFile));
		this.constants = new LinkedHashMap<String, String>();
	}
	
	public ConstantParser(String sourceFile, String destinationFile) {
		this(destinationFile);
		this.sourceFile = new File(sourceFile);
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(File sourceFile) {	
		this.sourceFile = sourceFile;
	}

	public File getDestinationFile() {
		return destinationFile;
	}

	public void setDestinationFile(File destinationFile) {
		this.destinationFile = destinationFile;
		this.interfaceName = destinationFile.getName().replaceFirst(Constants.REGEX_JAVA, Constants.EMPTY_STRING);
	}
	
	public String getInterfaceName() {
		return this.interfaceName;
	}

	public Map<String,String> loadConstant(){
		try {
			scan = new Scanner(destinationFile);
			String value = null;
			while (scan.hasNextLine()) {
				if((value = scan.findInLine(valuePattern))!=null){
					constants.put(scan.findInLine(keyPattern), value);
				}
				scan.nextLine();
			}
			scan.close();
			System.out.println(constants);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return constants;
	}
	
	public Map<String, String> replaceConstants(String importStatement){
		String line = null;
		File file = null;
		try {
			file = new File(Constants.TEMP_FILE);
			PrintWriter temp = new PrintWriter(file);
			scan = new Scanner(sourceFile);
			while((line = scan.nextLine()).contains(Constants.PACKAGE_NAME)){
				temp.write(line+Constants.CARRIAGE_RETURN);
			}
			temp.write(Constants.CARRIAGE_RETURN+importStatement+Constants.CARRIAGE_RETURN+line+Constants.CARRIAGE_RETURN);
			
			while(scan.hasNextLine()) {
				line = checkConstant(scan.nextLine());
				temp.write(line+Constants.CARRIAGE_RETURN);
			}
			temp.close();
			scan.close();
			sourceFile.delete();
			System.out.println(file.renameTo(sourceFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return constants;
	}
	
	public String checkConstant(String line){
		String token = null;
		Scanner scanLine = new Scanner(line);
		while ((token = scanLine.findInLine(keyPattern)) != null) {
			if(!constants.containsKey(token)){
				flag = true;
				constants.put(token, token.toUpperCase().replaceAll(Constants.REGEX_SLASH,Constants.EMPTY_STRING).replaceAll(Constants.REGEX_PATTERN_1, Constants.UNDER_SCORE));
			} 
			line = line.replaceAll(token, interfaceName+constants.get(token));
			System.out.println(token);
		}
		scanLine.close();
		return line;
	}
	
	public void insertConstants() throws IOException{
		if(flag){
			RandomAccessFile file = new RandomAccessFile(destinationFile, Constants.FMODE_RW);
			while(!file.readLine().contains(Constants.OPEN_BRACES));
			for(String key : constants.keySet()){
				file.writeBytes(String.format(Constants.FORMAT_STRING,constants.get(key), key));
			}
			file.writeBytes(Constants.CLOSE_BRACES);
			file.close();
		}
	}
	
	public static void main(String args[]) throws IOException {
		// destination file should be an existing interface file.
		ConstantParser parser = new ConstantParser("Target.java",
				"Constants.java");
		parser.loadConstant();
		parser.replaceConstants("import org.wb.proc.constants.Constants;");
		parser.insertConstants();
	}
}
