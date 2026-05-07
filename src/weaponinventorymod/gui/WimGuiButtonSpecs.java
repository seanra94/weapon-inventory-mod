package weaponinventorymod.gui;

import java.util.ArrayList;
import java.util.List;

final class WimGuiButtonSpecs {
    private WimGuiButtonSpecs() {
    }

    @SafeVarargs
    static <A> List<WimGuiButtonSpec<A>> of(WimGuiButtonSpec<A>... specs) {
        List<WimGuiButtonSpec<A>> result = new ArrayList<WimGuiButtonSpec<A>>();
        if (specs == null) {
            return result;
        }
        for (int i = 0; i < specs.length; i++) {
            if (specs[i] != null) {
                result.add(specs[i]);
            }
        }
        return result;
    }
}
