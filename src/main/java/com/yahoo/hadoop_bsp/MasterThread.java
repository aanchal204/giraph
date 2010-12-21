package com.yahoo.hadoop_bsp;

import org.apache.log4j.Logger;

/**
 * Master thread that will coordinate the activities of the tasks.  It runs
 * on all task processes, however, will only execute its algorithm if it knows
 * it is the "leader" from Zookeeper.
 * @author aching
 *
 */
public class MasterThread<I> extends Thread {
	/** Class logger */
    private static final Logger LOG = Logger.getLogger(BspService.class);
	/** Reference to shared BspService */
	private CentralizedServiceMaster<I> m_bspService = null;
	/** Constructor */
	MasterThread(BspService<I> bspService) {
		m_bspService = bspService;
	}

	/**
	 * The master algorithm.  The algorithm should be able to withstand
	 * failures and resume as necessary since the master may switch during a 
	 * job.
	 */
	@Override
	public void run() {
		try {
		    if (m_bspService.becomeMaster() == false) {
		        return;
		    }
			m_bspService.masterSetJobState(BspService.State.RUNNING);
			int partitions = m_bspService.masterCreatePartitions();
			long superStep = m_bspService.getSuperStep();
			while (m_bspService.masterBarrier(superStep, partitions) == false) {
				LOG.info("masterThread: Finished a barrier at superstep " + 
						 superStep);
				++superStep;
			}
			m_bspService.masterSetJobState(BspService.State.FINISHED);
			m_bspService.masterCleanup(partitions);
		} catch (Exception e) {
			LOG.error("masterThread: Master algorithm failed: " + 
					  e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
