package org.adaway.db;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import android.content.Context;

import androidx.annotation.NonNull;

import org.adaway.db.converter.DateConverter;
import org.adaway.db.converter.ListTypeConverter;
import org.adaway.db.dao.HostListItemDao;
import org.adaway.db.dao.HostsSourceDao;
import org.adaway.db.entity.HostListItem;
import org.adaway.db.entity.HostsSource;
import org.adaway.provider.RoomMigrationHelper;
import org.adaway.util.AppExecutors;

/**
 * This class is the application database based on Room.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
@Database(entities = {HostsSource.class, HostListItem.class}, version = 1)
@TypeConverters({DateConverter.class, ListTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    /**
     * The database singleton instance.
     */
    private static volatile AppDatabase instance;

    /**
     * Get the database instance.
     *
     * @param context The application context.
     * @return The database instance.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app.db"
                    ).addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            AppExecutors.getInstance().diskIO().execute(
                                    () -> {
                                        RoomMigrationHelper.migrateToRoom(context, instance);
                                        AppDatabase.initialize(instance);
                                    }
                            );
                        }
                    }).build();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the database content.
     */
    private static void initialize(AppDatabase database) {
        // Check if there is no hosts source
        HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        if (!hostsSourceDao.getAll().isEmpty()) {
            return;
        }
        // AdAway's own mobile hosts
        HostsSource source1 = new HostsSource();
        source1.setUrl("https://adaway.org/hosts.txt");
        source1.setEnabled(true);
        hostsSourceDao.insert(source1);
        // https://github.com/StevenBlack/hosts
        HostsSource source2 = new HostsSource();
        source2.setUrl("https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts");
        source2.setEnabled(true);
        hostsSourceDao.insert(source2);
        // https://pgl.yoyo.org/adservers/
        HostsSource source3 = new HostsSource();
        source3.setUrl("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext");
        source3.setEnabled(true);
        hostsSourceDao.insert(source3);
    }

    /**
     * Get the hosts source DAO.
     *
     * @return The hosts source DAO.
     */
    public abstract HostsSourceDao hostsSourceDao();

    /**
     * Get the hosts list item DAO.
     *
     * @return The hosts list item DAO.
     */
    public abstract HostListItemDao hostsListItemDao();
}
