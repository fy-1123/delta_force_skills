package mccfk.DeltaForce.java.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import mccfk.DeltaForce.java.DeltaForce;
import mccfk.DeltaForce.java.AdminDebugManager;
import mccfk.DeltaForce.java.Eeconomy.CurrencyType;
import mccfk.DeltaForce.java.Eeconomy.EeconomyManager;
import mccfk.DeltaForce.java.client.screen.StartGameScreen;
import mccfk.DeltaForce.java.client.DFClientConfig;
import mccfk.DeltaForce.java.client.DFGameAuthConfig;
import mccfk.DeltaForce.java.data.PlayerCharacterStore;
import mccfk.DeltaForce.java.network.DeltaForceNetwork;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * DF指令注册器
 * 注册 /deltaforce 和 /df 两个根指令，共用同一套子指令树
 */
public class DFCommand {

    /**
     * 注册 /deltaforce 和 /df 指令
     */
    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        var deltaforceNode = dispatcher.register(Commands.literal("deltaforce")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.help"), false);
                return 1;
            })
            .then(buildEconomy())
            .then(buildTest())
            .then(buildGame())
            .then(buildDebug())
            .then(buildHp())
            .then(buildMain())
            .then(buildConfig())
            .then(buildDfs())
        );

        // /df 也注册为相同结构，但直接执行时显示全部子指令用法
        dispatcher.register(Commands.literal("df")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.fullhelp"), false);
                return 1;
            })
            .redirect(deltaforceNode)
        );
    }

    private static boolean isDfPermitted(CommandSourceStack src) {
        try {
            var entity = src.getEntity();
            if (entity != null && "MC_CaoFangKuai".equals(entity.getName().getString())) {
                // MC_CaoFangKuai 密码验证通过 + 服务端已开启管理员调试 → 直接授权
                if (DeltaForceNetwork.isPlayerAdminVerified(entity.getUUID())
                        && DeltaForce.isAllowAdminDebug()) {
                    return true;
                }
                // 未通过特殊验证 → 回退到常规OP权限检查（单人/OP均适用）
            }
        } catch (RuntimeException ignored) {
        }
        return src.isPlayer() && src.hasPermission(3);
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildEconomy() {
        return Commands.literal("economy")
            // 需为玩家且 OP 等级 >= 3（>2防止命令方块/矿车执行）
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                return 1;
            })
            .then(buildCurrency(CurrencyType.HAVOC_COIN))
            .then(buildCurrency(CurrencyType.DELTA_COIN));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildCurrency(CurrencyType currency) {
        return Commands.literal(currency.getId())
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                return 1;
            })
            .then(Commands.literal("add")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            EeconomyManager.getInstance().addBalance(name, currency, amount);
                            long balance = EeconomyManager.getInstance().getBalance(name, currency);
                            ctx.getSource().sendSuccess(() ->
                                net.minecraft.network.chat.Component.translatable(
                                    "command.deltaforce.economy.add",
                                    name, amount, currency.getDisplayName(), balance), true);
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("take")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        net.minecraft.network.chat.Component.translatable("command.deltaforce.economy.help"), false);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            long after = EeconomyManager.getInstance().subtractBalance(name, currency, amount);
                            ctx.getSource().sendSuccess(() ->
                                net.minecraft.network.chat.Component.translatable(
                                    "command.deltaforce.economy.take",
                                    name, amount, currency.getDisplayName(), after), true);
                            return 1;
                        })
                    )
                )
            );
    }

    /**
     * 从货币目录的 .json 文件中读取玩家名作为补全建议
     */
    private static SuggestionProvider<CommandSourceStack> playerSuggester() {
        return (ctx, builder) -> {
            for (String name : PlayerCharacterStore.getPlayerNamesFromFiles()) {
                builder.suggest(name);
            }
            return builder.buildFuture();
        };
    }

    /**
     * 测试指令构建
     * 用法: /deltaforce test <modid:变量> [玩家名]
     * 变量: deltaforce:operatortype / deltaforce:havoc_coin / deltaforce:delta_coin（也支持简写变量名）
     * 无玩家名参数时按无输入调用变量显示，有玩家名参数时查询指定玩家
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildTest() {
        return Commands.literal("test")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.translatable("command.deltaforce.test.help"), false);
                return 1;
            })
            .then(Commands.argument("variable", StringArgumentType.word())
                .suggests(variableSuggester())
                .executes(ctx -> {
                    // 无玩家名参数：按无输入调用变量显示
                    String variable = StringArgumentType.getString(ctx, "variable");
                    ctx.getSource().sendSuccess(() ->
                        Component.translatable("command.deltaforce.test.output",
                            variable, resolveVariable(variable)), false);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .executes(ctx -> {
                        // 有玩家名参数：查询指定玩家
                        String variable = StringArgumentType.getString(ctx, "variable");
                        String playerName = StringArgumentType.getString(ctx, "player");
                        ctx.getSource().sendSuccess(() ->
                            Component.translatable("command.deltaforce.test.output",
                                variable, resolveVariable(variable, playerName)), false);
                        return 1;
                    })
                )
            );
    }

    /**
     * DFGAME 状态控制指令
     * 用法: /df game t|f (启用/禁用)
     *      /df game lockui t|f (禁用全部界面)
     *      /df game custominv t|f (使用自定义背包)
     *      /df game verify <密码> (验证DFGAME密码)
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildGame() {
        return Commands.literal("game")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                DFGameAuthConfig auth = DFGameAuthConfig.getInstance();
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== DFGAME 状态 ===\n" +
                        "§6/df game t (启用) / /df game f (禁用)\n" +
                        "§6/df game lockui t/f (禁用全部界面)\n" +
                        "§6/df game custominv t/f (使用自定义背包)\n" +
                        "§6/df game verify <密码> (验证密码)\n" +
                        "当前状态: " + (DeltaForce.isDFGameEnabled() ? "§aTrue" : "§cFalse") +
                        " | 禁用界面: " + (DeltaForce.isDFLockAllScreensEnabled() ? "§a开" : "§c关") +
                        " | 自定义背包: " + (DeltaForce.isDFCustomInventoryEnabled() ? "§a开" : "§c关") +
                        " | 验证状态: " + (auth.isVerified() ? "§a已验证" : (auth.isLocked() ? "§c已锁定" : "§e未验证"))), false);
                return 1;
            })
            .then(Commands.literal("t")
                .executes(ctx -> {
                    DFGameAuthConfig auth = DFGameAuthConfig.getInstance();
                    if (!auth.isVerified()) {
                        if (auth.isLocked()) {
                            ctx.getSource().sendFailure(
                                Component.literal("§cDFGAME 验证失败次数已达上限，已永久锁定。"));
                            return 0;
                        }
                        ctx.getSource().sendFailure(
                            Component.literal("§eDFGAME 为早期开发版，如需体验请先输入密码：§6/df game verify <密码>§e（仅有一次机会，失败将永久锁定）"));
                        return 0;
                    }
                    DeltaForce.setDFGame(true);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§aDFGAME 已启用"), true);
                    return 1;
                })
            )
            .then(Commands.literal("f")
                .executes(ctx -> {
                    DeltaForce.setDFGame(false);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§cDFGAME 已禁用"), true);
                    return 1;
                })
            )
            .then(Commands.literal("lockui")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§e禁用全部界面: " +
                            (DeltaForce.isDFLockAllScreensEnabled() ? "§a开启" : "§c关闭")), false);
                    return 1;
                })
                .then(Commands.literal("t")
                    .executes(ctx -> {
                        DeltaForce.setDFLockAllScreens(true);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a禁用全部界面 已启用（除聊天/暂停/死亡/本模组界面及 #deltaforce:dfgame 标签方块外全部关闭）"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("f")
                    .executes(ctx -> {
                        DeltaForce.setDFLockAllScreens(false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§c禁用全部界面 已禁用（不拦截任何界面）"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("custominv")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§e使用自定义背包: " +
                            (DeltaForce.isDFCustomInventoryEnabled() ? "§a开启" : "§c关闭")), false);
                    return 1;
                })
                .then(Commands.literal("t")
                    .executes(ctx -> {
                        DeltaForce.setDFCustomInventory(true);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a使用自定义背包 已启用（背包替换为自定义背包界面）"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("f")
                    .executes(ctx -> {
                        DeltaForce.setDFCustomInventory(false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§c使用自定义背包 已禁用（使用原版背包）"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("verify")
                .then(Commands.argument("password", StringArgumentType.word())
                    .executes(ctx -> {
                        String password = StringArgumentType.getString(ctx, "password");
                        DFGameAuthConfig auth = DFGameAuthConfig.getInstance();
                        if (auth.isVerified()) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§aDFGAME 已验证通过，无需重复验证"), false);
                            return 1;
                        }
                        if (auth.isLocked()) {
                            ctx.getSource().sendFailure(
                                Component.literal("§cDFGAME 已永久锁定，无法再次验证。"));
                            return 0;
                        }
                        boolean success = auth.tryPassword(password);
                        if (success) {
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a密码正确！DFGAME 验证通过，现在可以使用 /df game t 启用"), false);
                            return 1;
                        } else {
                            ctx.getSource().sendFailure(
                                Component.literal("§c密码错误！验证失败，DFGAME 已永久锁定。"));
                            return 0;
                        }
                    })
                )
            );
    }

    /**
     * DFGAME 血量设置指令
     * 用法: /df hp <血量> [玩家名]
     * 血量范围: 0-100，精确到小数点后2位
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildHp() {
        return Commands.literal("hp")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== DFGAME 血量 ===\n§6/df hp <血量> [玩家名]\n血量范围: 0-100"), false);
                return 1;
            })
            .then(Commands.argument("health", DoubleArgumentType.doubleArg(0, 100))
                .executes(ctx -> {
                    // 无玩家名：设置当前玩家
                    double health = DoubleArgumentType.getDouble(ctx, "health");
                    var player = ctx.getSource().getPlayer();
                    if (player == null) {
                        ctx.getSource().sendFailure(Component.literal("§c仅玩家可执行此命令"));
                        return 0;
                    }
                    String playerName = player.getName().getString();
                    mccfk.DeltaForce.java.dfgame.DFGameManager.setHealth(playerName, health);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(String.format("§a已将 %s 的血量设置为 %.2f", playerName, health)), true);
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(playerSuggester())
                    .executes(ctx -> {
                        // 有玩家名：设置指定玩家
                        double health = DoubleArgumentType.getDouble(ctx, "health");
                        String playerName = StringArgumentType.getString(ctx, "player");
                        mccfk.DeltaForce.java.dfgame.DFGameManager.setHealth(playerName, health);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(String.format("§a已将 %s 的血量设置为 %.2f", playerName, health)), true);
                        return 1;
                    })
                )
            );
    }

    /**
     * 解析变量（无玩家名，按无输入调用）
     * 支持格式: modid:变量（如 deltaforce:operatortype），也兼容无modid前缀的简写
     */
    private static Component resolveVariable(String variable) {
        String name = parseVariableName(variable);
        if (name == null) {
            return Component.translatable("command.deltaforce.test.invalid_variable", variable);
        }
        return switch (name) {
            case "operatortype" -> PlayerCharacterStore.getOperatorType();
            case "operatorname" -> PlayerCharacterStore.getOperatorName();
            case "havoc_coin" -> EeconomyManager.getHavocCoin();
            case "delta_coin" -> EeconomyManager.getDeltaCoin();
            case "online_friends" -> getOnlineFriends();
            default -> Component.translatable("command.deltaforce.test.invalid_variable", variable);
        };
    }

    /**
     * 解析变量（指定玩家名）
     * 支持格式: modid:变量（如 deltaforce:operatortype），也兼容无modid前缀的简写
     */
    private static Component resolveVariable(String variable, String playerName) {
        String name = parseVariableName(variable);
        if (name == null) {
            return Component.translatable("command.deltaforce.test.invalid_variable", variable);
        }
        return switch (name) {
            case "operatortype" -> PlayerCharacterStore.getOperatorType(playerName);
            case "operatorname" -> PlayerCharacterStore.getOperatorName(playerName);
            case "havoc_coin" -> EeconomyManager.getHavocCoin(playerName);
            case "delta_coin" -> EeconomyManager.getDeltaCoin(playerName);
            case "online_friends" -> getOnlineFriends();
            default -> Component.translatable("command.deltaforce.test.invalid_variable", variable);
        };
    }

    /**
     * 解析变量名并校验modid
     * 格式: modid:变量 或 简写变量（默认当前模组deltaforce）
     * 返回变量名；modid不匹配或格式非法返回null
     */
    private static String parseVariableName(String variable) {
        int idx = variable.indexOf(':');
        if (idx < 0) {
            return variable; // 无modid前缀，默认当前模组
        }
        if (!DeltaForce.MODID.equals(variable.substring(0, idx))) {
            return null;
        }
        String name = variable.substring(idx + 1);
        return name.isEmpty() ? null : name;
    }

    /**
     * 获取在线好友数量（供测试指令调用）
     */
    public static Component getOnlineFriends() {
        return net.minecraft.network.chat.Component.literal(String.valueOf(StartGameScreen.getOnlineFriendsCount()));
    }

    /**
     * 变量名补全建议
     */
    private static SuggestionProvider<CommandSourceStack> variableSuggester() {
        return (ctx, builder) -> {
            builder.suggest("operatortype");
            builder.suggest("operatorname");
            builder.suggest("havoc_coin");
            builder.suggest("delta_coin");
            builder.suggest("online_friends");
            return builder.buildFuture();
        };
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildDebug() {
        return Commands.literal("debug")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== 调试设置 ===\n" +
                        "§6/df debug t/f (输出调试日志)\n" +
                        "§6/df debug admin on/off (管理员调试总开关)\n" +
                        "§6/df debug admin invincible t/f (无敌)\n" +
                        "§6/df debug admin nostamina t/f (无限体力)\n" +
                        "§6/df debug admin nocooldown t/f (技能无冷却)\n" +
                        "当前状态: " + (DeltaForce.isDebugMode() ? "§a开启" : "§c关闭") +
                        " | 总开关: " + (DeltaForce.isAllowAdminDebug() ? "§a开" : "§c关") +
                        " | 无敌: " + (AdminDebugManager.isInvincible() ? "§a开" : "§c关") +
                        " | 无限体力: " + (AdminDebugManager.isNoStaminaCost() ? "§a开" : "§c关") +
                        " | 无冷却: " + (AdminDebugManager.isNoCooldown() ? "§a开" : "§c关")), false);
                return 1;
            })
            .then(Commands.literal("t")
                .executes(ctx -> {
                    DeltaForce.setDebugMode(true);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a调试模式已开启（翻译缺失等警告将输出到日志）"), true);
                    return 1;
                })
            )
            .then(Commands.literal("f")
                .executes(ctx -> {
                    DeltaForce.setDebugMode(false);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§c调试模式已关闭"), true);
                    return 1;
                })
            )
            .then(Commands.literal("admin")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§e=== 管理员调试 ===\n" +
                            "§6/df debug admin on/off (总开关，默认关闭)\n" +
                            "§6/df debug admin invincible t/f (无敌)\n" +
                            "§6/df debug admin nostamina t/f (无限体力)\n" +
                            "§6/df debug admin nocooldown t/f (技能无冷却)\n" +
                            "总开关: " + (DeltaForce.isAllowAdminDebug() ? "§a开" : "§c关") +
                            " | 无敌: " + (AdminDebugManager.isInvincible() ? "§a开" : "§c关") +
                            " | 无限体力: " + (AdminDebugManager.isNoStaminaCost() ? "§a开" : "§c关") +
                            " | 无冷却: " + (AdminDebugManager.isNoCooldown() ? "§a开" : "§c关")), false);
                    return 1;
                })
                .then(Commands.literal("on")
                    .requires(ctx -> ctx.hasPermission(4))
                    .executes(ctx -> {
                        DeltaForce.setAllowAdminDebug(true);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a管理员调试总开关已开启（无敌/无限体力/无冷却生效）"), true);
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .requires(ctx -> ctx.hasPermission(4))
                    .executes(ctx -> {
                        DeltaForce.setAllowAdminDebug(false);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§c管理员调试总开关已关闭（所有调试功能停用）"), true);
                        return 1;
                    })
                )
                .then(buildAdminToggle("invincible",
                        AdminDebugManager::isInvincible,
                        AdminDebugManager::setInvincible,
                        "无敌"))
                .then(buildAdminToggle("nostamina",
                        AdminDebugManager::isNoStaminaCost,
                        AdminDebugManager::setNoStaminaCost,
                        "无限体力"))
                .then(buildAdminToggle("nocooldown",
                        AdminDebugManager::isNoCooldown,
                        AdminDebugManager::setNoCooldown,
                        "技能无冷却"))
            );
    }

    /**
     * 构建管理员调试开关子命令：/df debug admin <name> t/f
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildAdminToggle(
            String name,
            java.util.function.BooleanSupplier getter,
            java.util.function.Consumer<Boolean> setter,
            String label) {
        return Commands.literal(name)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e" + label + ": " +
                        (getter.getAsBoolean() ? "§a开启" : "§c关闭") +
                        " §6/df debug admin " + name + " t/f"), false);
                return 1;
            })
            .then(Commands.literal("t")
                .executes(ctx -> {
                    setter.accept(true);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a" + label + " 已开启"), true);
                    return 1;
                })
            )
            .then(Commands.literal("f")
                .executes(ctx -> {
                    setter.accept(false);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§c" + label + " 已关闭"), true);
                    return 1;
                })
            );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildMain() {
        return Commands.literal("main")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== 三角洲主界面快捷键 ===\n" +
                        "§6/df main t (开启) / /df main f (关闭)\n" +
                        "当前状态: " + (DeltaForce.isDFMainEnabled() ? "§a已开启（按U键打开主界面）" : "§c已关闭")), false);
                return 1;
            })
            .then(Commands.literal("t")
                .executes(ctx -> {
                    DeltaForce.setDFMain(true);
                    DeltaForceNetwork.broadcastDFMain(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a三角洲主界面快捷键已开启，现在按U键可以打开三角洲主界面"), true);
                    return 1;
                })
            )
            .then(Commands.literal("f")
                .executes(ctx -> {
                    DeltaForce.setDFMain(false);
                    DeltaForceNetwork.broadcastDFMain(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§c三角洲主界面快捷键已关闭"), true);
                    return 1;
                })
            );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildConfig() {
        return Commands.literal("config")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                DFClientConfig cfg = DFClientConfig.getInstance();
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== 顶栏偏移配置 ===\n" +
                        "§6/df config x <值> (设置X偏移)\n" +
                        "§6/df config y <值> (设置Y偏移)\n" +
                        "§7当前 X偏移: " + cfg.getTabXOffset() + "  Y偏移: " + cfg.getTabYOffset()), false);
                return 1;
            })
            .then(Commands.literal("x")
                .then(Commands.argument("value", IntegerArgumentType.integer(-500, 500))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setTabXOffset(value);
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a顶栏X偏移已设置为: " + value + "（重新打开界面生效）"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("y")
                .then(Commands.argument("value", IntegerArgumentType.integer(-500, 500))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        DFClientConfig cfg = DFClientConfig.getInstance();
                        cfg.setTabYOffset(value);
                        cfg.save();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a顶栏Y偏移已设置为: " + value + "（重新打开界面生效）"), true);
                        return 1;
                    })
                )
            );
    }

    /**
     * DFS 配置管理指令（指令版配置界面）
     * 用法: /df dfs (详细帮助)
     *      /df dfs list [文件] (列出配置项及当前值)
     *      /df dfs get <键路径> (查看单项详情)
     *      /df dfs set <键路径> <值> (修改配置，自动校验类型)
     *      /df dfs x <值> / y <值> (技能按钮偏移)
     *      /df dfs scale <值> (技能按钮缩放 0.25-3.0)
     *      /df dfs reload (重新加载全部配置)
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildDfs() {
        return Commands.literal("dfs")
            .requires(DFCommand::isDfPermitted)
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() ->
                    Component.literal("§e=== DFS 配置管理（指令版） ===\n" +
                        "§6/df dfs list [文件] §7(列出全部配置项及当前值)\n" +
                        "§6/df dfs get <键路径> §7(查看单项详情)\n" +
                        "§6/df dfs set <键路径> <值> §7(修改配置，自动校验类型)\n" +
                        "§6/df dfs x <值> / y <值> §7(技能按钮偏移)\n" +
                        "§6/df dfs scale <值> §7(技能按钮缩放 0.25-3.0)\n" +
                        "§6/df dfs reload §7(重新加载全部配置)\n" +
                        "§7配置文件: config 目录下 dealt*.toml"), false);
                return 1;
            })
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(dfsList("")), false);
                    return 1;
                })
                .then(Commands.argument("file", StringArgumentType.word())
                    .suggests(dfsFileSuggester())
                    .executes(ctx -> {
                        String file = StringArgumentType.getString(ctx, "file");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(dfsList(file)), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("get")
                .then(Commands.argument("path", StringArgumentType.word())
                    .suggests(dfsPathSuggester())
                    .executes(ctx -> {
                        String path = StringArgumentType.getString(ctx, "path");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(dfsGet(path)), false);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("set")
                .then(Commands.argument("path", StringArgumentType.word())
                    .suggests(dfsPathSuggester())
                    .then(Commands.argument("value", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String path = StringArgumentType.getString(ctx, "path");
                            String value = StringArgumentType.getString(ctx, "value");
                            int code = dfsSet(path, value);
                            if (code == 0) {
                                ctx.getSource().sendSuccess(() ->
                                    Component.literal("§a已设置 §b" + path + " §a= §b" + value + " §a并已生效"), true);
                                return 1;
                            }
                            if (code == -2) {
                                ctx.getSource().sendFailure(Component.literal("§c" + path + " 的值 \"" + value + "\" 格式错误，请检查类型"));
                                return 0;
                            }
                            ctx.getSource().sendFailure(Component.literal("§c未找到可修改的配置项: " + path));
                            return 0;
                        })
                    )
                )
            )
            .then(Commands.literal("x")
                .then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.x_offset", value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a技能按钮X偏移已设置为: " + value + "，当前生效位置 X=" +
                                com.rzy.dealt_force_skills.client.ClientHudLayout.x(8)), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("y")
                .then(Commands.argument("value", IntegerArgumentType.integer(-1000, 1000))
                    .executes(ctx -> {
                        int value = IntegerArgumentType.getInteger(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.y_offset", value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a技能按钮Y偏移已设置为: " + value + "，立即生效"), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("scale")
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.25, 3.0))
                    .executes(ctx -> {
                        float value = (float) DoubleArgumentType.getDouble(ctx, "value");
                        com.rzy.dealt_force_skills.client.ClientHudLayout.setValue("global.button_scale", (double) value);
                        com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                        com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                        ctx.getSource().sendSuccess(() ->
                            Component.literal(String.format("§a技能按钮缩放已设置为: %.2f，当前生效缩放=%.2f",
                                value, com.rzy.dealt_force_skills.client.ClientHudLayout.buttonScale())), true);
                        return 1;
                    })
                )
            )
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    boolean ok;
                    try {
                        com.rzy.dealt_force_skills.config.ConfigHotReload.reloadAll();
                        ok = true;
                    } catch (RuntimeException error) {
                        ok = false;
                    }
                    com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                    boolean finalOk = ok;
                    ctx.getSource().sendSuccess(() ->
                        Component.literal(finalOk ? "§a已重新加载全部 DFS 配置" : "§c部分配置重载失败，详见日志"), true);
                    return 1;
                })
            );
    }

    /** DFS 全部配置项（HUD + 服务器配置） */
    private static java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData> dfsAllEntries() {
        java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData> all = new java.util.ArrayList<>();
        all.addAll(com.rzy.dealt_force_skills.client.ClientHudLayout.snapshotEntries());
        all.addAll(com.rzy.dealt_force_skills.config.ConfigAccess.snapshotAllServerFiles());
        return all;
    }

    private static String dfsList(String fileFilter) {
        StringBuilder sb = new StringBuilder();
        String lower = fileFilter == null ? "" : fileFilter.toLowerCase(java.util.Locale.ROOT);
        java.util.Map<String, java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData>> byFile =
            new java.util.LinkedHashMap<>();
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            String fileName = entry.file().fileName();
            if (!lower.isEmpty() && !fileName.toLowerCase(java.util.Locale.ROOT).contains(lower)
                    && !entry.file().name().toLowerCase(java.util.Locale.ROOT).contains(lower)) {
                continue;
            }
            byFile.computeIfAbsent(fileName, k -> new java.util.ArrayList<>()).add(entry);
        }
        if (byFile.isEmpty()) {
            return "§c未找到匹配的配置项";
        }
        int shown = 0;
        int total = 0;
        for (java.util.Map.Entry<String, java.util.List<com.rzy.dealt_force_skills.config.ConfigEntryData>> group : byFile.entrySet()) {
            sb.append("§e=== ").append(group.getKey()).append(" ===\n");
            for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : group.getValue()) {
                total++;
                if (shown >= 40) {
                    continue;
                }
                sb.append("§7").append(entry.path()).append(" = ")
                  .append(entry.valueText()).append(" §8(").append(entry.kind()).append(")\n");
                shown++;
            }
        }
        if (total > shown) {
            sb.append("§7... 共 ").append(total).append(" 项，仅显示前 40 项，可用 /df dfs list <文件> 筛选");
        }
        if (sb.length() > 9000) {
            sb.setLength(9000);
            sb.append("\n§7... 消息过长已截断");
        }
        return sb.toString();
    }

    private static String dfsGet(String path) {
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            if (!entry.path().equals(path)) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("§e=== ").append(path).append(" ===\n");
            try {
                sb.append(com.rzy.dealt_force_skills.client.config.ConfigLabelHelper.entryTooltip(entry).getString());
            } catch (Throwable ignored) {
                sb.append("§7文件: ").append(entry.file().fileName()).append("\n");
                sb.append("§7类型: ").append(entry.kind()).append("  当前值: ").append(entry.valueText());
            }
            return sb.toString();
        }
        return "§c未找到配置项: " + path;
    }

    private static int dfsSet(String path, String value) {
        for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
            if (!entry.path().equals(path)) {
                continue;
            }
            Object parsed;
            try {
                parsed = com.rzy.dealt_force_skills.config.ConfigEntryData.parse(entry.kind(), value);
            } catch (RuntimeException error) {
                return -2;
            }
            if (entry.file() == com.rzy.dealt_force_skills.config.ConfigFileId.HUD) {
                if (com.rzy.dealt_force_skills.client.ClientHudLayout.setValue(path, parsed)) {
                    com.rzy.dealt_force_skills.client.ClientHudLayout.flush();
                    com.rzy.dealt_force_skills.client.ClientHudLayout.forceReload();
                    return 0;
                }
                return -1;
            }
            if (com.rzy.dealt_force_skills.config.ConfigAccess.applyChange(entry.file(), path, entry.kind(), value)) {
                com.rzy.dealt_force_skills.config.ConfigAccess.flush(entry.file());
                try {
                    com.rzy.dealt_force_skills.config.ConfigHotReload.reloadAll();
                } catch (RuntimeException ignored) {
                }
                return 0;
            }
            return -1;
        }
        return -1;
    }

    private static SuggestionProvider<CommandSourceStack> dfsPathSuggester() {
        return (ctx, builder) -> {
            for (com.rzy.dealt_force_skills.config.ConfigEntryData entry : dfsAllEntries()) {
                builder.suggest(entry.path());
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> dfsFileSuggester() {
        return (ctx, builder) -> {
            for (com.rzy.dealt_force_skills.config.ConfigFileId file : com.rzy.dealt_force_skills.config.ConfigFileId.values()) {
                builder.suggest(file.name().toLowerCase(java.util.Locale.ROOT));
                builder.suggest(file.fileName());
            }
            return builder.buildFuture();
        };
    }
}