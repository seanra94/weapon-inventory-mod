package weaponsprocurement.ui

import java.util.ArrayList

class WimGuiButtonSpecs private constructor() {
    companion object {
        @JvmStatic
        fun <A> of(vararg specs: WimGuiButtonSpec<A>?): List<WimGuiButtonSpec<A>> {
            val result = ArrayList<WimGuiButtonSpec<A>>()
            for (spec in specs) {
                if (spec != null) result.add(spec)
            }
            return result
        }
    }
}
