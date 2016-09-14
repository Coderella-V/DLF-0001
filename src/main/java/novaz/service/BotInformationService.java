package novaz.service;

import novaz.core.AbstractService;
import novaz.main.NovaBot;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

import java.util.List;

/**
 * updates information posts about the bot and clears the channel (if possible)
 */
public class BotInformationService extends AbstractService {

	public BotInformationService(NovaBot b) {
		super(b);
	}

	@Override
	public String getIdentifier() {
		return "bot_information_display";
	}

	@Override
	public long getDelayBetweenRuns() {
		return 86400000L;
	}

	@Override
	public boolean shouldIRun() {
		return true;
	}

	@Override
	public void beforeRun() {
	}

	@Override
	public void run() {
		List<IChannel> subscribedChannels = getSubscribedChannels();
		IUser me = bot.instance.getOurUser();
		for (IChannel channel : subscribedChannels) {
			bot.commandHandler.getCommand("purge").execute(new String[]{}, channel, me);
			bot.out.sendMessage(channel,bot.commandHandler.getCommand("info").execute(new String[]{}, channel, me));
			bot.out.sendMessage(channel,bot.commandHandler.getCommand("help").execute(new String[]{}, channel, me));
		}
	}

	@Override
	public void afterRun() {
	}
}