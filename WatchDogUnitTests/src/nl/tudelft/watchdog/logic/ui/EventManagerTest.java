package nl.tudelft.watchdog.logic.ui;

import nl.tudelft.watchdog.logic.document.DocumentFactory;
import nl.tudelft.watchdog.logic.interval.IntervalManager;
import nl.tudelft.watchdog.logic.interval.IntervalPersister;
import nl.tudelft.watchdog.logic.interval.intervaltypes.IntervalBase;
import nl.tudelft.watchdog.logic.interval.intervaltypes.ReadingInterval;
import nl.tudelft.watchdog.logic.interval.intervaltypes.TypingInterval;
import nl.tudelft.watchdog.logic.ui.WatchDogEvent.EventType;

import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests the {@link EventManager}. Because this creates the intervals that are
 * eventually transfered to the server, this is one of the most crucial parts of
 * WatchDog. Tests could flicker because they deal with timers (and Java gives
 * no guarantee as to when these timers will be executed).
 */
public class EventManagerTest {

	private static final int USER_ACTIVITY_TIMEOUT = 300;
	private static final int TIMEOUT_GRACE_PERIOD = (int) (USER_ACTIVITY_TIMEOUT * 1.1);
	private EventManager eventManager;
	private IntervalManager intervalManager;

	@Before
	public void setup() {
		DocumentFactory documentFactoryMock = Mockito
				.mock(DocumentFactory.class);
		IntervalManager intervalManagerReal = new IntervalManager(
				Mockito.mock(IntervalPersister.class), documentFactoryMock);
		intervalManager = Mockito.spy(intervalManagerReal);
		eventManager = new EventManager(intervalManager, documentFactoryMock,
				USER_ACTIVITY_TIMEOUT);
	}

	@Test
	public void testCreateReadInterval() {
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(ReadingInterval.class));
	}

	@Test
	public void testCreateReadIntervalOnlyOnce() {
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(ReadingInterval.class));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		eventManager.update(createMockEvent(EventType.PAINT));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(ReadingInterval.class));
	}

	@Test
	public void testReadIntervalIsClosed() {
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(ReadingInterval.class));
		eventManager.update(createMockEvent(EventType.END_FOCUS));
		Mockito.verify(intervalManager, Mockito.atLeastOnce()).closeInterval(
				Mockito.isA(ReadingInterval.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void testCreateWriteInterval() {
		eventManager.update(createMockEvent(EventType.EDIT));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(TypingInterval.class));
	}

	@Test
	public void testCreateWriteIntervalOnlyOnce() {
		eventManager.update(createMockEvent(EventType.EDIT));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(TypingInterval.class));
		Mockito.verify(intervalManager, Mockito.never())
				.addAndSetEditorInterval(Mockito.isA(ReadingInterval.class));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		eventManager.update(createMockEvent(EventType.EDIT));
		eventManager.update(createMockEvent(EventType.PAINT));
		Mockito.verify(intervalManager).addAndSetEditorInterval(
				Mockito.isA(TypingInterval.class));
	}

	@Test
	public void testWritingIntervalsGetClosedOnHigherCancel() {
		eventManager.update(createMockEvent(EventType.EDIT));
		eventManager.update(createMockEvent(EventType.END_ECLIPSE));
		Mockito.verify(intervalManager).closeInterval(
				Mockito.isA(TypingInterval.class));
	}

	@Test
	public void testTimeoutWorksForRegularIntervals() {
		eventManager.update(createMockEvent(EventType.ACTIVE_WINDOW));
		eventManager.update(createMockEvent(EventType.USER_ACTIVITY));
		Mockito.verify(intervalManager, Mockito.timeout(TIMEOUT_GRACE_PERIOD))
				.closeInterval(Mockito.isA(IntervalBase.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void testTimeoutWorksForReadingIntervals() {
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager, Mockito.timeout(TIMEOUT_GRACE_PERIOD))
				.closeInterval(Mockito.isA(ReadingInterval.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void testTimeoutWorksForWritingIntervals() {
		eventManager.update(createMockEvent(EventType.EDIT));
		// first close null interval
		Mockito.verify(intervalManager).closeInterval(
				Mockito.isNull(IntervalBase.class));
		Mockito.verify(intervalManager, Mockito.timeout(TIMEOUT_GRACE_PERIOD))
				.closeInterval(Mockito.isA(TypingInterval.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void testReadingTimeoutIsProlonged() {
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager,
				Mockito.timeout(USER_ACTIVITY_TIMEOUT / 2).never())
				.closeInterval(Mockito.any(IntervalBase.class));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD).never()).closeInterval(
				Mockito.any(IntervalBase.class));
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD * 2)).closeInterval(
				Mockito.isA(ReadingInterval.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	@Test
	public void userInactiveShouldNotCloseReading() {
		eventManager.update(createMockEvent(EventType.USER_ACTIVITY));
		eventManager.update(createMockEvent(EventType.ACTIVE_FOCUS));
		Mockito.verify(intervalManager,
				Mockito.timeout(USER_ACTIVITY_TIMEOUT / 2).never())
				.closeInterval(Mockito.any(IntervalBase.class));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		Mockito.verify(intervalManager,
				Mockito.timeout(TIMEOUT_GRACE_PERIOD).never()).closeInterval(
				Mockito.any(IntervalBase.class));
		eventManager.update(createMockEvent(EventType.CARET_MOVED));
		Mockito.verify(intervalManager,
				Mockito.timeout((int) (TIMEOUT_GRACE_PERIOD * 2.4)).never())
				.closeInterval(Mockito.any(IntervalBase.class));
		Mockito.verify(intervalManager,
				Mockito.timeout((int) (TIMEOUT_GRACE_PERIOD * 3.5))).closeInterval(
				Mockito.isA(ReadingInterval.class));
		Assert.assertEquals(null, intervalManager.getEditorInterval());
	}

	private WatchDogEvent createMockEvent(EventType eventType) {
		return new WatchDogEvent(Mockito.mock(ITextEditor.class), eventType);
	}
}