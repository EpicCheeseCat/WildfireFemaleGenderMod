/*
    Wildfire's Female Gender Mod is a female gender mod created for Minecraft.
    Copyright (C) 2023 WildfireRomeo

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package com.wildfire.gui.screen;

import com.wildfire.gui.GuiUtils;
import com.wildfire.main.Gender;
import com.wildfire.main.WildfireGender;

import java.util.*;

import com.wildfire.gui.WildfireButton;
import com.wildfire.main.cloud.CloudSync;
import com.wildfire.main.entitydata.PlayerConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.realms.dto.PlayerInfo;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.dedicated.gui.PlayerListGui;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Nullables;
import net.minecraft.world.GameMode;

@Environment(EnvType.CLIENT)
public class WardrobeBrowserScreen extends BaseWildfireScreen {
	private static final Identifier BACKGROUND_FEMALE = Identifier.of(WildfireGender.MODID, "textures/gui/wardrobe_bg2.png");
	private static final Identifier BACKGROUND = Identifier.of(WildfireGender.MODID, "textures/gui/wardrobe_bg3.png");
	private static final Identifier TXTR_RIBBON = Identifier.of(WildfireGender.MODID, "textures/bc_ribbon.png");
	private static final Identifier CLOUD_ICON = Identifier.of(WildfireGender.MODID, "textures/cloud.png");

	private static final boolean isBreastCancerAwarenessMonth = Calendar.getInstance().get(Calendar.MONTH) == Calendar.OCTOBER;

	public WardrobeBrowserScreen(Screen parent, UUID uuid) {
		super(Text.translatable("wildfire_gender.wardrobe.title"), parent, uuid);
	}

	@Override
  	public void init() {
		final var client = Objects.requireNonNull(this.client);
	    int y = this.height / 2;
		PlayerConfig plr = Objects.requireNonNull(getPlayer(), "getPlayer()");

		this.addDrawableChild(new WildfireButton(this.width / 2 - 42, y - 52, 158, 20, getGenderLabel(plr.getGender()), button -> {
			Gender gender = switch (plr.getGender()) {
				case MALE -> Gender.FEMALE;
				case FEMALE -> Gender.OTHER;
				case OTHER -> Gender.MALE;
			};
			if (plr.updateGender(gender)) {
				button.setMessage(getGenderLabel(gender));
				PlayerConfig.saveGenderInfo(plr);
				clearAndInit();
			}
		}));

		if (plr.getGender().canHaveBreasts()) {
			this.addDrawableChild(new WildfireButton(this.width / 2 - 42, y - 32, 158, 20, Text.translatable("wildfire_gender.appearance_settings.title").append("..."),
					button -> client.setScreen(new WildfireBreastCustomizationScreen(WardrobeBrowserScreen.this, this.playerUUID))));
		}
		this.addDrawableChild(new WildfireButton(this.width / 2 - 42, y - (plr.getGender().canHaveBreasts() ? 12 : 32), 158, 20, Text.translatable("wildfire_gender.char_settings.title").append("..."),
				button -> client.setScreen(new WildfireCharacterSettingsScreen(WardrobeBrowserScreen.this, this.playerUUID))));

		//noinspection ExtractMethodRecommender
		var cloud = new WildfireButton(
				this.width / 2 + 97, y - 63, 12, 9, Text.translatable("wildfire_gender.cloud_settings"),
				button -> client.setScreen(new WildfireCloudSyncScreen(this, this.playerUUID))
		) {
			@Override
			protected void drawInner(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
				ctx.drawTexture(RenderLayer::getGuiTextured, CLOUD_ICON, getX() + 1, getY() + 1, 0, 0, 10, 7, 17, 13, 16, 13);
			}
		};

		if(!CloudSync.isAvailable()) {
			cloud.setTooltip(Tooltip.of(Text.translatable("wildfire_gender.cloud.unavailable_offline")));
			cloud.setActive(false);
		} else {
			cloud.setTooltip(Tooltip.of(Text.translatable("wildfire_gender.cloud.available_online")));
		}

		this.addDrawableChild(cloud);
		this.addDrawableChild(new WildfireButton(this.width / 2 + 111, y - 63, 9, 9, Text.literal("X"),
			button -> close(), text -> GuiUtils.doneNarrationText()));

	    super.init();
  	}

	private Text getGenderLabel(Gender gender) {
		return Text.translatable("wildfire_gender.label.gender").append(" - ").append(gender.getDisplayName());
	}

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		this.renderInGameBackground(ctx);

		PlayerConfig plr = getPlayer();
		if(plr == null) return;
		Identifier backgroundTexture = plr.getGender().canHaveBreasts() ? BACKGROUND_FEMALE : BACKGROUND;
		ctx.drawTexture(RenderLayer::getGuiTextured, backgroundTexture, (this.width - 248) / 2, (this.height - 134) / 2, 0, 0, 248, 156, 256, 256);

		if(client != null && client.world != null) {
			int xP = this.width / 2 - 82;
			int yP = this.height / 2 + 40;
			PlayerEntity ent = client.world.getPlayerByUuid(this.playerUUID);
			if(ent != null) {
				ctx.enableScissor(xP - 35, yP - 93, xP + 35, yP + 6);
				GuiUtils.drawEntityOnScreen(ctx, xP, yP, 45, (xP - mouseX), (yP - 76 - mouseY), ent);
				ctx.disableScissor();
			}
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		int x = this.width / 2;
	    int y = this.height / 2;
		ctx.drawText(textRenderer, title, x - 118, y - 62, 4473924, false);

		if(client != null && client.player != null) {
			boolean withCreator = client.player.networkHandler.getPlayerList().stream()
					.anyMatch((player) -> player.getProfile().getId().equals(WildfireGender.CREATOR_UUID));
			boolean withContributor = client.player.networkHandler.getPlayerList().stream()
					.anyMatch(player -> WildfireGender.CONTRIBUTOR_UUIDS.contains(player.getProfile().getId()));

			int creatorY = y + 65;

			// move down so we don't overlap with the breast cancer awareness month banner
			if(isBreastCancerAwarenessMonth) creatorY += 30;

			if(withCreator && withContributor) { //with both the creator and a contributor
				GuiUtils.drawCenteredTextWrapped(ctx, this.textRenderer, Text.translatable("wildfire_gender.label.with_both"), this.width / 2, creatorY, 250, 0xFF00FF);
			} else if(withCreator) { //only with the creator
				GuiUtils.drawCenteredText(ctx, this.textRenderer, Text.translatable("wildfire_gender.label.with_creator"), this.width / 2, creatorY, 0xFF00FF);
			} else if(withContributor) { //only with a contributor
				GuiUtils.drawCenteredText(ctx, this.textRenderer, Text.translatable("wildfire_gender.label.with_contributor"), this.width / 2, creatorY, 0xFF00FF);
			}

			List<PlayerListEntry> syncedPlayers = collectPlayerEntries();
			if(!syncedPlayers.isEmpty()) {
				ctx.drawText(textRenderer, Text.translatable("wildfire_gender.wardrobe.players_using_mod").formatted(Formatting.AQUA), 5, 5, 0xFFFFFF, false);
				int yPos = 18;
				for(PlayerListEntry entry : syncedPlayers) {
					PlayerConfig cfg = WildfireGender.getPlayerById(entry.getProfile().getId());
					ctx.drawText(textRenderer, Text.literal(entry.getProfile().getName() + " - ").append(cfg.getGender().getDisplayName()), 10, yPos, 0xFFFFFF, false);
					yPos += 10;
				}
			}
		}

		if(isBreastCancerAwarenessMonth) {
			int bcaY = y - 45;
			ctx.fill(x - 159, bcaY + 106, x + 159, bcaY + 136, 0x55000000);
			ctx.drawTextWithShadow(textRenderer, Text.translatable("wildfire_gender.cancer_awareness.title").formatted(Formatting.BOLD, Formatting.ITALIC), this.width / 2 - 148, bcaY + 117, 0xFFFFFF);
			ctx.drawTexture(RenderLayer::getGuiTextured, TXTR_RIBBON, x + 130, bcaY + 109, 0, 0, 26, 26, 20, 20, 20, 20);
		}
	}


	private List<PlayerListEntry> collectPlayerEntries() {
		return this.client.player.networkHandler.getListedPlayerListEntries().stream()
				.filter(entry -> !entry.getProfile().getId().equals(client.player.getUuid()))
				.filter(entry -> {
					var cfg = WildfireGender.getPlayerById(entry.getProfile().getId());
					return cfg != null && cfg.getSyncStatus() != PlayerConfig.SyncStatus.UNKNOWN;
				})
				.limit(40L)
				.toList();
	}
}