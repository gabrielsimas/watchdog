package nl.tudelft.watchdog.eclipse.logic.ui.listeners;

import nl.tudelft.watchdog.core.logic.ui.events.EditorEvent;
import nl.tudelft.watchdog.core.logic.ui.events.WatchDogEvent.EventType;
import nl.tudelft.watchdog.eclipse.logic.ui.EventManager;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/** Enriches an {@link IEditorPart} for all user-triggered events. */
public class EditorListener {
	private final ITextEditor editor;
	private final EventManager eventManager;

	private IDocument document;
	private IDocumentListener documentListener;
	private CaretListener caretListener;
	private FocusListener focusListener;
	private StyledText styledText;
	private PaintListener paintListener;

	/** Enriches the supplied editor with all suitable listeners. */
	public EditorListener(EventManager eventManager, ITextEditor editor) {
		this.eventManager = eventManager;
		this.editor = editor;
		listenToDocumentChanges();
		listenToEditorScrolling();
	}

	/**
	 * Adds a document change listener to the supplied editor. Fires a
	 * {@link StartEditingEditorEvent} when a change to a document is made.
	 * 
	 * @param partEditor
	 * @throws IllegalArgumentException
	 */
	private void listenToDocumentChanges() throws IllegalArgumentException {
		IDocumentProvider documentProvider = editor.getDocumentProvider();
		document = documentProvider.getDocument(editor.getEditorInput());
		documentListener = new IDocumentListener() {

			@Override
			public void documentChanged(DocumentEvent event) {
				eventManager.update(new EditorEvent(editor,
						EventType.SUBSEQUENT_EDIT));
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				eventManager.update(new EditorEvent(editor,
						EventType.START_EDIT));
			}
		};
		document.addDocumentListener(documentListener);
	}

	private void listenToEditorScrolling() {
		styledText = (StyledText) editor.getAdapter(Control.class);
		if (styledText == null) {
			return;
		}
		// creates a listener for when the user moves the caret (cursor)
		caretListener = new CaretListener() {
			@Override
			public void caretMoved(CaretEvent event) {
				eventManager.update(new EditorEvent(editor,
						EventType.CARET_MOVED));
				// cursor place changed
			}
		};
		styledText.addCaretListener(caretListener);

		// creates a listener for redraws of the view, e.g. when scrolled
		paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				eventManager.update(new EditorEvent(editor, EventType.PAINT));
			}
		};
		styledText.addPaintListener(paintListener);

		focusListener = new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				eventManager.update(new EditorEvent(editor,
						EventType.ACTIVE_FOCUS));
			}
		};
		styledText.addFocusListener(focusListener);
	}

	/** Removes all listeners registered with this editor. */
	public void removeListeners() {
		document.removeDocumentListener(documentListener);
		if (styledText == null) {
			return;
		}
		styledText.removePaintListener(paintListener);
		styledText.removeCaretListener(caretListener);
		styledText.removeFocusListener(focusListener);
	}
}
