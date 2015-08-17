package org.jRTClamScan;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FolderMonitor implements Runnable {
	private String folder=null;
	private int corepoolsize=1;
	private int maxpoolsize=5;
	private long keepalive=10;
	private String exclude=null;
	private long maxfilesize=20971520;
	private int watchtimer=10;
	
	private Thread thread=null;
	private FileAlterationMonitor monitor=null;
	private static final Logger logger=LogManager.getLogger(FolderMonitor.class);
	private ThreadPoolExecutor executorPool=null;
	private static HashMap<File,Thread> watchedfiles=new HashMap<File,Thread>();
	private boolean isRunning=false;
	
	public String getFolder() {
		return folder;
	}
	
	public void setFolder(String folder) {
		this.folder=folder;
	}
	
	public int getCorepoolsize() {
		return corepoolsize;
	}
	
	public void setCorepoolsize(int corepoolsize) {
		this.corepoolsize=corepoolsize;
	}
	
	public int getMaxpoolsize() {
		return maxpoolsize;
	}
	
	public void setMaxpoolsize(int maxpoolsize) {
		this.maxpoolsize=maxpoolsize;
	}
	
	public long getKeepalive() {
		return keepalive;
	}
	
	public void setKeepalive(long keepalive) {
		this.keepalive=keepalive;
	}
	
	public String getExclude() {
		return exclude;
	}
	
	public void setExclude(String exclude) {
		this.exclude=exclude;
	}
	
	public long getMaxfilesize() {
		return maxfilesize;
	}
	
	public void setMaxfilesize(long maxfilesize) {
		this.maxfilesize=maxfilesize;
	}
	
	public int getWatchtimer() {
		return watchtimer;
	}
	
	public void setWatchtimer(int watchtimer) {
		this.watchtimer=watchtimer;
	}
	
	public boolean getIsrunning() {
		return this.isRunning;
	}
	
	public void start() {
		File dir=new File(folder);
		if(dir.exists()) {
			initThreadPoolExecutor();
			thread=new Thread(this);
			thread.start();
		}
		else
			logger.warn("Folder does not exist: "+folder);
	}
	
	private void initThreadPoolExecutor() {
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>() {
    	    private static final long serialVersionUID = -6903933921423432194L;
    	    @Override
    	    public boolean offer(Runnable e) {
    	        if (size() <= 1)
    	            return super.offer(e);
    	        
    	        return false;
    	    }
    	};
    	executorPool=new ThreadPoolExecutor(corepoolsize,maxpoolsize,keepalive, TimeUnit.SECONDS, queue,Executors.defaultThreadFactory());
    	executorPool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				try {
					executor.getQueue().put(r);
				} catch (InterruptedException e) {
					logger.error("Unable to initialize thread pool: "+e.getMessage());
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		});
	}
	
	private boolean wathchingFile(File scanfile) {
		boolean ret=false;
		
		if(watchedfiles.isEmpty())
			ret=false;
		else {
			Set<Entry<File, Thread>> set=watchedfiles.entrySet();
			Iterator<Entry<File, Thread>> iterator=set.iterator();
			while(iterator.hasNext()) {
				Entry<File, Thread> entry=(Entry<File,Thread>)iterator.next();
				File file=(File)entry.getKey();
				if(file.getAbsolutePath().equals(scanfile.getAbsolutePath())) {
					ret=true;
					break;
				}
			}
		}
		
		if(ret==true)
			logger.debug("Watcher running on file: "+scanfile.getAbsolutePath());
		else
			logger.debug("Watcher not running on file: "+scanfile.getAbsolutePath());
		
		return ret;
	}
	
	private Thread getWatchThread(File scanfile) {
		Thread scanthread=null;
		Set<Entry<File, Thread>> set=watchedfiles.entrySet();
		Iterator<Entry<File, Thread>> iterator=set.iterator();
		watchedfiles=new HashMap<File,Thread>();
		while(iterator.hasNext()) {
			Entry<File, Thread> entry=(Entry<File,Thread>)iterator.next();
			File file=(File)entry.getKey();
			if(file.getAbsolutePath().equals(scanfile.getAbsolutePath())) {
				scanthread=(Thread)entry.getValue();
			}
			else
				watchedfiles.put(entry.getKey(), entry.getValue());
		}
		
		logger.debug("Watcher thread found for file: "+scanfile.getAbsolutePath());
		
		return scanthread;
	}

	public void run() {
		this.isRunning=true;
		FileAlterationObserver fao=new FileAlterationObserver(folder);
		fao.addListener(new FileAlterationListener() {
			public void onFileDelete(File file) {}
			public void onDirectoryDelete(File directory) {}
			public void onDirectoryCreate(File directory) {}
			public void onDirectoryChange(File directory) {}
			public void onStart(FileAlterationObserver observer) {}
			public void onStop(FileAlterationObserver observer) {}
			
			private void processfile(final File file) {
				if(file.length()==0)
					logger.debug("File is empty: "+file.getAbsolutePath());
				else
					if(exclude!=null && file.getName().matches(exclude))
						logger.info("File matches exclusion regex: "+file.getAbsolutePath());
					else
						if(file.length()>maxfilesize)
							logger.info("File bigger than max file size allowed: "+file.getAbsolutePath());
						else {
							if(wathchingFile(file)) {
								Thread scanthread=getWatchThread(file);
								scanthread.interrupt();
							}
							
							Thread scanthread=new Thread(new Runnable() {
								
								@Override
								public void run() {
									try {
										logger.debug("Started watcher on file: "+file.getAbsolutePath());
										Thread.sleep(watchtimer*1000);
										logger.debug("Stopped watcher on file: "+file.getAbsolutePath());
										executorPool.execute(new ClamScan(file));
									} catch (InterruptedException e) {
										logger.debug("Resetting Watcher on file: "+file.getAbsolutePath());
									}
								}
							});
							scanthread.start();
							
							watchedfiles.put(file, scanthread);
						}
			}
			
			public void onFileCreate(File file) {
				logger.debug("New file created: "+file.getAbsolutePath());
				processfile(file);
			}
			
			public void onFileChange(File file) {
				logger.debug("File modified: "+file.getAbsolutePath());
				processfile(file);
			}
		});
		monitor=new FileAlterationMonitor();
		monitor.addObserver(fao);
		
		logger.info("Starting monitor on "+folder);
		try {
			monitor.start();
		} catch (Exception e) {
			logger.error("Unable to start monitor: "+e.getMessage());
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
	
	public void stop() {
		logger.info("Stopping monitor on "+folder);
		try {
			executorPool.shutdown();
			monitor.stop();
		} catch (Exception e) {
			logger.error("Unable to stop monitor: "+e.getMessage());
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

}
