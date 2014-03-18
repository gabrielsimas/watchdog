package nl.tudelft.watchdog.uitests;

import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for whether the plugin starts up and initializes the workbench with the
 * correct icons and menus.
 */
public class PluginStartupTest extends EclipseWorkbenchInitializer {

	/** Tests whether the command menu contribution from WatchDog active. */
	@Test
	public void testCommandMenuContribution() {
		SWTBotMenu fileMenu = bot.menu("WatchDog");
		Assert.assertNotNull(fileMenu);
	}

	/** Tests whether the toolbar menu contribution from WatchDog active. */
	@Test
	public void testToolbarMenuContribution() {
		SWTBotToolbarButton button = bot
				.toolbarButtonWithTooltip("WatchDog is active and recording ...");
		Assert.assertNotNull(button);
	}

}