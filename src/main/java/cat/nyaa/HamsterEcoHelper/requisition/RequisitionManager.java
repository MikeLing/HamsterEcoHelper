package cat.nyaa.HamsterEcoHelper.requisition;

import cat.nyaa.HamsterEcoHelper.HamsterEcoHelper;
import cat.nyaa.HamsterEcoHelper.I18n;
import cat.nyaa.HamsterEcoHelper.utils.Message;
import cat.nyaa.HamsterEcoHelper.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class RequisitionManager extends BukkitRunnable {
    private final HamsterEcoHelper plugin;
    private RequisitionInstance currentReq = null;
    public RequisitionManager(HamsterEcoHelper plugin) {
        this.plugin = plugin;
        int interval = plugin.config.requisitionIntervalTicks;
        runTaskTimer(plugin, interval, interval);
    }

    @Override
    public void run() {
        if (Bukkit.getOnlinePlayers().size() < plugin.config.requisitionMinimalPlayer) {
            plugin.logger.info(I18n.get("internal.info.req_not_enough_player", Bukkit.getOnlinePlayers().size(), plugin.config.auctionMinimalPlayer));
            return;
        }
        int delay = Utils.inclusiveRandomInt(0, plugin.config.requisitionMaxDelayTicks);
        (new BukkitRunnable() {
            @Override
            public void run() {
                newRequisition();
            }
        }).runTaskLater(plugin, delay);
        plugin.logger.info(I18n.get("internal.info.req_scheduled", delay));
    }

    public boolean newRequisition() {
        if (currentReq != null) return false;
        if (plugin.config.itemsForReq.isEmpty()) return false;
        RequisitionSpecification item = Utils.randomWithWeight(
                plugin.config.itemsForReq,
                (RequisitionSpecification i) -> i.randomWeight
        );
        if (item == null) return false;
        return newRequisition(item);
    }

    public boolean newRequisition(RequisitionSpecification item) {
        if (currentReq != null) return false;
        if (item == null) return false;

        int unitPrice = Utils.inclusiveRandomInt(item.minPurchasePrice, item.maxPurchasePrice);
        int amount = item.maxAmount < 0? -1: Utils.inclusiveRandomInt(item.minAmount, item.maxAmount);
        currentReq = new RequisitionInstance(item, unitPrice, amount, plugin, ()->this.currentReq=null);
        if (plugin.config.requisitionHintInterval > 0) {
            currentReq.new RequisitionHintTimer(this, plugin.config.requisitionHintInterval, plugin);
        }
        return true;
    }

    public void halt() {
        if (currentReq != null) {
            currentReq.halt();
            currentReq = null;
            new Message(I18n.get("user.req.halted")).broadcast("heh.sell");
        }
    }

    public RequisitionInstance getCurrentRequisition() {
        return currentReq;
    }
}
