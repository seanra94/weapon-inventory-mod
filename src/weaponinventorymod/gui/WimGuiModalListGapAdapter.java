package weaponinventorymod.gui;

final class WimGuiModalListGapAdapter<A> implements WimGuiModalListLayout.ExtraGapProvider<WimGuiListRow<A>> {
    private final WimGuiModalListRenderer.ExtraGapProvider<A> provider;

    WimGuiModalListGapAdapter(WimGuiModalListRenderer.ExtraGapProvider<A> provider) {
        this.provider = provider;
    }

    @Override
    public float extraGapBefore(WimGuiListRow<A> row) {
        return provider == null ? 0f : provider.extraGapBefore(row);
    }
}
