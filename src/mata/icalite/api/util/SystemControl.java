package mata.icalite.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.sql.Timestamp;

public class SystemControl {
	public String createBatExec(String command, String outfileDir) throws Exception{
		String outfilename = outfileDir + "\\runme.bat";
		boolean writeAppend = false;
		String pid = "not found";
		String testCommand = "cmd /c " + outfilename;
		
		new FileManager().fileWriter(outfilename, command, writeAppend);
		
		Runtime.getRuntime().exec(testCommand);
		pid = ManagementFactory.getRuntimeMXBean().getName();
		
		return pid;
	}
	
	public String createBatExec2(String command) throws Exception{
		String outfilename = "runme.bat";
		boolean writeAppend = false;
		String pid = "not found";
		
		String commandFinal = "@echo off\r\n"
				+ "rem there is a tab in the file at the end of the line below\r\n"
				+ "set tab=    \r\n"
				+ "set cmd=test.bat\r\n"
				+ "set dir=%~dp0\r\n\r\n"
				+ "echo Starting MyProg\r\n"
				+ "set pid=notfound\r\n\r\n"
				+ "for /F \"usebackq tokens=1,2 delims=;=%tab% \" %%i in (\r\n"
						+ "	`wmic process call create \"%cmd%\"^, \"%dir%\"`\r\n"
								+ ") do (\r\n"
								+ "	if /I %%i EQU ProcessId (\r\n"
								+ "		set pid=%%j\r\n"
								+ "	)\r\n"
								+ ")\r\n\r\n"
								+ "echo %pid% > MyProg.pid\r\n";
		
		new FileManager().fileWriter(outfilename, commandFinal, writeAppend);
		
		Runtime.getRuntime().exec(outfilename);
		pid = ManagementFactory.getRuntimeMXBean().getName();
		
		return pid;
	}
	
	public String runExec(String command) throws IOException, InterruptedException {
		StringBuffer output = new StringBuffer();
 
		Process p;
		p = Runtime.getRuntime().exec(command);
//			System.out.println(ManagementFactory.getRuntimeMXBean().getName());
		p.waitFor();
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
 
        String line = "";			
		while ((line = reader.readLine())!= null) {
			output.append(line + "\n");
		}
	
		return output.toString();
	}

	public String getStackTrace(Exception e){
		StringWriter errors = new StringWriter();
		e.printStackTrace(new PrintWriter(errors));
		return errors.toString().replace("&", "&amp;").
				replace("\"", "&quot;").
				replace("'","&apos;").
				replace("<", "&lt;").
				replace(">", "&gt;");
	}
	
	public static String getCurrentTime(){
		 java.util.Date date= new java.util.Date();
		 return (new Timestamp(date.getTime()).toString());
	}
}
