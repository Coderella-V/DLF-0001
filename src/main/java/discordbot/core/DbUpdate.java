package discordbot.core;

import discordbot.db.IDbVersion;
import discordbot.db.MySQLAdapter;
import discordbot.main.Launcher;
import org.reflections.Reflections;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DbUpdate {
	private final MySQLAdapter adapter;
	private int highestVersion = 0;
	private Map<Integer, IDbVersion> versionMap;

	public DbUpdate(MySQLAdapter adapter) {
		this.adapter = adapter;
		versionMap = new HashMap<>();
		collectDatabaseVersions();
	}

	private void collectDatabaseVersions() {
		Reflections reflections = new Reflections("discordbot.db.version");
		Set<Class<? extends IDbVersion>> classes = reflections.getSubTypesOf(IDbVersion.class);
		for (Class<? extends IDbVersion> s : classes) {
			try {
				IDbVersion iDbVersion = s.newInstance();
				highestVersion = Math.max(highestVersion, iDbVersion.getToVersion());
				versionMap.put(iDbVersion.getFromVersion(), iDbVersion);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean updateToCurrent() {
		try {
			int currentVersion = getCurrentVersion();
			if (currentVersion == -1) {
				System.out.println("Run the /sql/create.sql file on the database");
				Launcher.stop(ExitCode.GENERIC_ERROR);
			}
			if (currentVersion == highestVersion) {
				return true;
			}
			boolean hasUpgrade = versionMap.containsKey(currentVersion);
			while (hasUpgrade) {
				IDbVersion dbVersion = versionMap.get(currentVersion);
				for (String query : dbVersion.getExecutes()) {
					adapter.insert(query);
				}
				currentVersion = dbVersion.getToVersion();
				saveDbVersion(currentVersion);
				hasUpgrade = versionMap.containsKey(currentVersion);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	private int getCurrentVersion() throws SQLException {
		DatabaseMetaData metaData = adapter.getConnection().getMetaData();

		try (ResultSet rs = metaData.getTables(null, null, "commands", null)) {
			if (rs.next()) {
				System.out.println(rs.getString("table_name"));
			} else {
				return -1;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		try (ResultSet rs = metaData.getTables(null, null, "bot_meta", null)) {
			if (rs.next()) {
				System.out.println(rs.getString("table_name"));
			} else {
				return 0;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		try (ResultSet rs = adapter.select("SELECT * FROM bot_meta WHERE meta_name = ?", "db_version")) {
			if (rs.next()) {
				return Integer.parseInt(rs.getString("meta_value"));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return 0;
	}

	private void saveDbVersion(int version) throws SQLException {
		adapter.insert("INSERT INTO bot_meta(meta_name, meta_value) VALUES (?,?) ON DUPLICATE KEY UPDATE meta_value = ? ", "db_version", version, version);
	}
}