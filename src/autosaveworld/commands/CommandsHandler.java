/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package autosaveworld.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import autosaveworld.commands.subcommands.BackupSubCommand;
import autosaveworld.commands.subcommands.ForceGCSubCommand;
import autosaveworld.commands.subcommands.ForceRestartSubCommand;
import autosaveworld.commands.subcommands.HelpSubCommand;
import autosaveworld.commands.subcommands.LocaleSubCommand;
import autosaveworld.commands.subcommands.PluginManagerSubCommand;
import autosaveworld.commands.subcommands.ProcessManagerSubCommand;
import autosaveworld.commands.subcommands.PurgeSubCommand;
import autosaveworld.commands.subcommands.ReloadAllSubCommand;
import autosaveworld.commands.subcommands.ReloadConfigMSGSubCommand;
import autosaveworld.commands.subcommands.ReloadConfigSubCommand;
import autosaveworld.commands.subcommands.RestartSubCommand;
import autosaveworld.commands.subcommands.SaveSubCommand;
import autosaveworld.commands.subcommands.ServerStatusSubCommand;
import autosaveworld.commands.subcommands.VersionSubCommand;
import autosaveworld.commands.subcommands.WorldRegenSubCommand;
import autosaveworld.config.AutoSaveWorldConfig;
import autosaveworld.config.AutoSaveWorldConfigMSG;
import autosaveworld.config.LocaleChanger;
import autosaveworld.core.AutoSaveWorld;
import autosaveworld.core.logging.MessageLogger;

public class CommandsHandler implements TabExecutor {

	private final AutoSaveWorld plugin;
	private final AutoSaveWorldConfig config;
	private final AutoSaveWorldConfigMSG configmsg;
	private final LocaleChanger localeChanger;

	private final HashMap<String, ISubCommand> subcommandhandlers = new HashMap<String, ISubCommand>();

	public CommandsHandler(AutoSaveWorld plugin, AutoSaveWorldConfig config, AutoSaveWorldConfigMSG configmsg, LocaleChanger localeChanger) {
		this.plugin = plugin;
		this.config = config;
		this.configmsg = configmsg;
		this.localeChanger = localeChanger;
	}

	public void initSubCommandHandlers() {
		subcommandhandlers.clear();
		subcommandhandlers.put("version", new VersionSubCommand(plugin));
		subcommandhandlers.put("help", new HelpSubCommand());
		subcommandhandlers.put("reload", new ReloadAllSubCommand(config, configmsg));
		subcommandhandlers.put("reloadconfig", new ReloadConfigSubCommand(config));
		subcommandhandlers.put("reloadmsg", new ReloadConfigMSGSubCommand(configmsg));
		subcommandhandlers.put("locale", new LocaleSubCommand(localeChanger));
		subcommandhandlers.put("process", new ProcessManagerSubCommand(plugin));
		subcommandhandlers.put("pmanager", new PluginManagerSubCommand(plugin));
		subcommandhandlers.put("forcegc", new ForceGCSubCommand());
		subcommandhandlers.put("serverstatus", new ServerStatusSubCommand());
		subcommandhandlers.put("save", new SaveSubCommand(plugin));
		subcommandhandlers.put("backup", new BackupSubCommand(plugin));
		subcommandhandlers.put("purge", new PurgeSubCommand(plugin));
		subcommandhandlers.put("restart", new RestartSubCommand(plugin));
		subcommandhandlers.put("forcerestart", new ForceRestartSubCommand(plugin));
		subcommandhandlers.put("regenworld", new WorldRegenSubCommand(plugin, config, configmsg));
	}

	private PermissionCheck permCheck = new PermissionCheck();

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

		String commandName = command.getName().toLowerCase();

		// check permissions
		if (!permCheck.isAllowed(sender, commandName, args, config.commandOnlyFromConsole)) {
			MessageLogger.sendMessage(sender, configmsg.messageInsufficientPermissions);
			return true;
		}

		// now handle commands
		if (commandName.equalsIgnoreCase("autosave")) {
			// "autosave" command handler
			plugin.saveThread.startsave();
			return true;
		} else if (commandName.equalsIgnoreCase("autobackup")) {
			// "autobackup" command handler
			plugin.backupThread.startbackup();
			return true;
		} else if (commandName.equalsIgnoreCase("autopurge")) {
			// "autopurge" command handler
			plugin.purgeThread.startpurge();
			return true;
		} else if (commandName.equalsIgnoreCase("autosaveworld")) {
			// "autosaveworld" command handler
			if (args.length == 0) {
				return false;
			}
			String subcommand = args[0].toLowerCase();
			if (subcommandhandlers.containsKey(subcommand)) {
				ISubCommand handler = subcommandhandlers.get(subcommand);
				if (handler.getMinArguments() > args.length - 1) {
					MessageLogger.sendMessage(sender, "&4Not enough args");
					return true;
				} else {
					handler.handle(sender, Arrays.copyOfRange(args, 1, args.length));
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String commandLabel, String[] args) {
		ArrayList<String> result = new ArrayList<String>();
		if (command.getName().equals("autosaveworld")) {
			if (args.length == 1) {
				for (String subcommandname : subcommandhandlers.keySet()) {
					if (
						subcommandname.startsWith(args[0].toLowerCase()) &&
						permCheck.isAllowed(sender, command.getName(), new String[] {subcommandname}, config.commandOnlyFromConsole)
					) {
						result.add(subcommandname);
					}
				}
				return result;
			} else {
				String subcommandname = args[0].toLowerCase();
				if (!permCheck.isAllowed(sender, command.getName(), args, config.commandOnlyFromConsole)) {
					return new ArrayList<String>();
				}
				if (subcommandhandlers.containsKey(subcommandname)) {
					return subcommandhandlers.get(subcommandname).tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
				}
			}
		}
		return new ArrayList<String>();
	}

}
