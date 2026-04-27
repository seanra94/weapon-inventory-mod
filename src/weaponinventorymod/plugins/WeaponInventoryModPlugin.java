package weaponinventorymod.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import weaponinventorymod.listener.WeaponCargoTestIconProvider;

public class WeaponInventoryModPlugin extends BaseModPlugin {
    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);
        WeaponCargoTestIconProvider.register();
    }
}
