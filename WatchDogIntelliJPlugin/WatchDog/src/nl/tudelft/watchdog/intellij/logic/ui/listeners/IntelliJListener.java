package nl.tudelft.watchdog.intellij.logic.ui.listeners;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationActivationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import nl.tudelft.watchdog.intellij.logic.ui.EventManager;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType;

/**
 * Sets up the listeners for IntelliJ UI events and registers the shutdown
 * listeners.
 */
public class IntelliJListener {
    /**
     * The editorObservable.
     */
    private EventManager eventManager;

    private Disposable parent; //dummy disposable, needed for EditorFactory listener

    private final MessageBusConnection connection;

    private EditorWindowListener editorWindowListener;
    private GeneralActivityListener activityListener;

    /**
     * Constructor.
     */
    public IntelliJListener(EventManager userActionManager) {
        this.eventManager = userActionManager;

        parent = new Disposable() {
            @Override
            public void dispose() {
                // intentionally left empty
            }
        };

        editorWindowListener = new EditorWindowListener(eventManager);

        final MessageBus messageBus = ApplicationManager.getApplication().getMessageBus();
        connection = messageBus.connect();
    }

    /**
     * Adds IntelliJ listeners including already opened windows and
     * registers shutdown listeners.
     */
    public void attachListeners() {
        eventManager.update(new WatchDogEvent(this, EventType.START_IDE));

        connection.subscribe(ApplicationActivationListener.TOPIC,
                new IntelliJActivationListener(eventManager));
        activityListener = new GeneralActivityListener(eventManager);

        EditorFactory.getInstance().addEditorFactoryListener(editorWindowListener, parent);
    }

    public void removeListeners() {

        connection.disconnect();
        activityListener.removeListeners();
        parent.dispose(); // disposing this parent should remove EditorFactory listener
        EditorFactory.getInstance().removeEditorFactoryListener(editorWindowListener); //deprecated, but removes listener immediately
    }
}
