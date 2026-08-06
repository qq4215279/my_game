// package com.mumu.game.charge.listener;
//
// import com.mumu.game.core.utils.TimeUtil;
// import org.springframework.stereotype.Component;
//
//
// import jakarta.annotation.Resource;
//
// /** 充值事件监听 */
// @Component
// public class ChargeMonitor implements ITaskMonitor {
//   @Resource PlayerBaseDOOperator playerBaseDOOperator;
//   @Resource PlayerCurrencyOperator playerCurrencyOperator;
//
//   @Override
//   public boolean access(Player player, ActionType type) {
//     return type == ActionType.CHARGE;
//   }
//
//   @Override
//   public void accept(Player player, ActionData data) {
//     // 设置付费用户
//     PlayerAttributeEnum.CHARGED.set(player.getPlayerId(), Constants.FLAG_ON);
//
//     // 玩家未绑定facebook账号，触发充值时，发送邮件通知
//     if (!playerBaseDOOperator.isBind(player.getPlayerId(), ThirdPartyAccountEnum.FACEBOOK)) {
//       MailParams.build()
//           .setMailLanguageEnum(MailLanguageEnum.UNBIND_USER_CHARGE_TIP)
//           // 邮件锁，同一天只发送一次
//           .setLockKeys(
//               MailLanguageEnum.UNBIND_USER_CHARGE_TIP, player.getPlayerId(), DateUtil.dayOfYear())
//           .setLockExpire(TimeUtil.ONE_DAY_SECONDS)
//           .setExpiredDays(1)
//           .sendMail(player);
//     }
//   }
// }
