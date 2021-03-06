package nl.tudelft.watchdog.intellij.logic.ui.listeners;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestStatusListener;
import nl.tudelft.watchdog.intellij.logic.InitializationManager;
import nl.tudelft.watchdog.intellij.logic.interval.intervaltypes.JUnitInterval;
import nl.tudelft.watchdog.intellij.logic.ui.EventManager;
import nl.tudelft.watchdog.core.logic.ui.events.JUnitEvent;
import nl.tudelft.watchdog.core.util.WatchDogGlobals;

/** A listener to the execution of Junit test via the IDE. */
public class JUnitListener extends TestStatusListener {

    private EventManager eventManager;

    public JUnitListener() {
        if(WatchDogGlobals.isActive) {
            this.eventManager = InitializationManager.getInstance().getEventManager();
        }
    }

    @Override
    public void testSuiteFinished(AbstractTestProxy abstractTestProxy) {
        if(!WatchDogGlobals.isActive) return;
        if(eventManager == null) {
            this.eventManager = InitializationManager.getInstance().getEventManager();
        }
        JUnitInterval interval = new JUnitInterval(abstractTestProxy);
        eventManager.update(new JUnitEvent(interval));
    }
}
