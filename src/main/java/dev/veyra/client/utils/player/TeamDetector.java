package dev.veyra.client.utils.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;

/** Hypixel-aware teammate detection with vanilla scoreboard matching first. */
public final class TeamDetector {
    private TeamDetector() {
    }

    public static boolean areTeammates(EntityPlayer self, EntityPlayer target) {
        if (self == null || target == null || self == target) return self == target && self != null;

        Team selfTeam = self.getTeam();
        Team targetTeam = target.getTeam();
        if (selfTeam != null && selfTeam.isSameTeam(targetTeam)) return true;

        String selfTeamName = selfTeam == null ? null : selfTeam.getRegisteredName();
        String targetTeamName = targetTeam == null ? null : targetTeam.getRegisteredName();
        if (TeamMatcher.sameRegisteredTeam(selfTeamName, targetTeamName)) return true;

        // Hypixel can expose distinct scoreboard entries for players while keeping
        // the actual team in the prefix/name color. Only use this fallback when
        // both players have scoreboard teams, which avoids treating white FFA
        // names as teammates.
        if (selfTeam == null || targetTeam == null) return false;
        return TeamMatcher.sameEffectiveColor(
                colorPrefix(selfTeam), self.getDisplayName().getFormattedText(), self.getName(),
                colorPrefix(targetTeam), target.getDisplayName().getFormattedText(), target.getName());
    }

    private static String colorPrefix(Team team) {
        return team instanceof ScorePlayerTeam scoreTeam ? scoreTeam.getColorPrefix() : null;
    }
}
