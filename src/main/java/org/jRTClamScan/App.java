package org.jRTClamScan;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App 
{
	private static ArrayList<FolderMonitor> foldermonitors=null;
	private static final Logger logger=LogManager.getLogger(App.class);
	public static String CONF_FILE=null;
	public static Conf conf=null;
	
    public static void main( String[] args )
    {
        if(args.length!=1) {
        	System.out.println("Usage: java -jar jRTClamScan.jar config_file");
        	System.exit(1);
        }
        
        File config_file=new File(args[0]);
        if(!config_file.exists()) {
        	System.err.println(args[1]+" does not exist");
        	System.exit(1);
        }
        else {
        	CONF_FILE=args[0];
        	ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("classpath:context.xml");
        	ConfManager confmanager=(ConfManager)context.getBean("confmanagerBean");
        	confmanager.loadConf();
        	conf=confmanager.getConf();
        	context.close();        
        	
        	foldermonitors=conf.getFoldermonitors();
        	
        	for(int i=0;i<foldermonitors.size();i++)
        		foldermonitors.get(i).start();
        	
        	Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				
				public void run() {
					logger.info("Stopping all monitors");
					
					for(int i=0;i<foldermonitors.size();i++) {
						FolderMonitor fm=foldermonitors.get(i);
						if(fm.getIsrunning())
							fm.stop();
					}
				}
			}));
        }
    }
}