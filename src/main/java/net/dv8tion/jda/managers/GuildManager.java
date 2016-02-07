/**
 *    Copyright 2015-2016 Austin Keener & Michael Ritter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.managers;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.Region;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.entities.impl.JDAImpl;
import net.dv8tion.jda.entities.impl.UserImpl;
import net.dv8tion.jda.exceptions.GuildUnavailableException;
import net.dv8tion.jda.exceptions.PermissionException;
import net.dv8tion.jda.utils.AvatarUtil;
import net.dv8tion.jda.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * Manager used to modify aspects of a {@link net.dv8tion.jda.entities.Guild Guild}.
 */
public class GuildManager
{
    /**
     * Represents the idle time allowed until a user is moved to the
     * AFK {@link net.dv8tion.jda.entities.VoiceChannel} if one is set.
     */
    enum Timeout
    {
        SECONDS_60(60),
        SECONDS_300(300),
        SECONDS_900(900),
        SECONDS_1800(1800),
        SECONDS_3600(3600);

        private final int seconds;
        Timeout(int seconds)
        {
            this.seconds = seconds;
        }

        /**
         * The amount of seconds represented by this {@link net.dv8tion.jda.managers.GuildManager.Timeout}.
         *
         * @return
         *      An positive non-zero int representing the timeout amount in seconds.
         */
        public int getSeconds()
        {
            return seconds;
        }

        /**
         * The timeout as a string.<br>
         * Examples:    "60"  "300"   etc
         *
         * @return
         *      Seconds as a string.
         */
        @Override
        public String toString()
        {
            return "" + seconds;
        }
    }

    private final Guild guild;

    private Timeout timeout = null;
    private String name = null;
    private Region region = null;
    private AvatarUtil.Avatar icon = null;
    private String afkChannelId;

    private final Map<User, Set<Role>> addedRoles = new HashMap<>();
    private final Map<User, Set<Role>> removedRoles = new HashMap<>();

    /**
     * Creates a {@link net.dv8tion.jda.managers.GuildManager} that can be used to manage
     * different aspects of the provided {@link net.dv8tion.jda.entities.Guild}.
     *
     * @param guild
     *          The {@link net.dv8tion.jda.entities.Guild} which the manager deals with.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager(Guild guild)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        this.guild = guild;
        this.afkChannelId = guild.getAfkChannelId();
    }

    /**
     * Returns the {@link net.dv8tion.jda.entities.Guild Guild} object of this Manager. Useful if this Manager was returned via a create function
     *
     * @return
     *      the {@link net.dv8tion.jda.entities.Guild Guild} of this Manager
     */
    public Guild getGuild()
    {
        return guild;
    }

    /**
     * Changes the name of this Guild.
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param name
     *          the new name of the Guild, or null to keep current one
     * @return
     *      This {@link net.dv8tion.jda.managers.GuildManager GuildManager} instance. Useful for chaining.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager setName(String name)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_SERVER);

        if (guild.getName().equals(name))
        {
            this.name = null;
        }
        else
        {
            this.name = name;
        }
        return this;
    }

    /**
     * Changes the {@link net.dv8tion.jda.Region Region} of this {@link net.dv8tion.jda.entities.Guild Guild}.
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param region
     *          the new {@link net.dv8tion.jda.Region Region}, or null to keep current one
     * @return
     *      This {@link net.dv8tion.jda.managers.GuildManager GuildManager} instance. Useful for chaining.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager setRegion(Region region)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_SERVER);

        if (region == guild.getRegion() || region == Region.UNKNOWN)
        {
            this.region = null;
        }
        else
        {
            this.region = region;
        }
        return this;
    }

    /**
     * Changes the icon of this Guild.<br>
     * You can create the icon via the {@link net.dv8tion.jda.utils.AvatarUtil AvatarUtil} class.
     * Passing in null will keep the current icon,
     * while {@link net.dv8tion.jda.utils.AvatarUtil#DELETE_AVATAR DELETE_AVATAR} removes the current one.
     *
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param avatar
     *          the new icon, null to keep current, or AvatarUtil.DELETE_AVATAR to delete
     * @return
     *      This {@link net.dv8tion.jda.managers.GuildManager GuildManager} instance. Useful for chaining.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager setIcon(AvatarUtil.Avatar avatar)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_SERVER);

        this.icon = avatar;
        return this;
    }

    /**
     * Changes the AFK {@link net.dv8tion.jda.entities.VoiceChannel VoiceChannel} of this Guild
     * If passed null, this will disable the AFK-Channel.
     *
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param channel
     *          the new afk-channel
     * @return
     *      This {@link net.dv8tion.jda.managers.GuildManager GuildManager} instance. Useful for chaining.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager setAfkChannel(VoiceChannel channel)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_SERVER);

        if (channel != null && channel.getGuild() != guild)
        {
            throw new IllegalArgumentException("Given VoiceChannel is not member of modifying Guild");
        }
        this.afkChannelId = channel == null ? null : channel.getId();
        return this;
    }

    /**
     * Changes the AFK Timeout of this Guild
     * After given timeout (in seconds) Users being AFK in voice are being moved to the AFK-Channel
     * Valid timeouts are: 60, 300, 900, 1800, 3600.
     *
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param timeout
     *      the new afk timeout, or null to keep current one
     * @return
     *      This {@link net.dv8tion.jda.managers.GuildManager GuildManager} instance. Useful for chaining.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public GuildManager setAfkTimeout(Timeout timeout)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_SERVER);

        this.timeout = timeout;
        return this;
    }

    /**
     * Gives the {@link net.dv8tion.jda.entities.User User} the specified {@link net.dv8tion.jda.entities.Role Role}.<br>
     * If the {@link net.dv8tion.jda.entities.User User} already has the provided {@link net.dv8tion.jda.entities.Role Role}
     * this method will do nothing.
     *
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * @param user
     *          The {@link net.dv8tion.jda.entities.User User} that is gaining a new {@link net.dv8tion.jda.entities.Role Role}.
     * @param roles
     *          The {@link net.dv8tion.jda.entities.Role Roles} that are being assigned to the {@link net.dv8tion.jda.entities.User User}.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void addRoleToUser(User user, Role... roles)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_ROLES);

        Set<Role> addRoles = addedRoles.get(user);
        if (addRoles == null)
        {
            addRoles = new HashSet<>();
            addedRoles.put(user, addRoles);
        }
        Set<Role> removeRoles = removedRoles.get(user);
        if (removeRoles == null)
        {
            removeRoles = new HashSet<>();
            removedRoles.put(user, removeRoles);
        }
        for (Role role : roles)
        {
            if(guild.getPublicRole().equals(role))
                return;

            if (removeRoles.contains(role))
                removeRoles.remove(role);

            addRoles.add(role);
        }
    }

    /**
     * Removes the specified {@link net.dv8tion.jda.entities.Role Role} from the {@link net.dv8tion.jda.entities.User User}.<br>
     * If the {@link net.dv8tion.jda.entities.User User} does not have the specified {@link net.dv8tion.jda.entities.Role Role}
     * this method will do nothing.
     *
     * This change will only be applied, if {@link #update()} is called.
     * So multiple changes can be made at once.
     *
     * <b>NOTE:</b> you cannot remove the {@link net.dv8tion.jda.entities.Guild Guild} public role from a {@link net.dv8tion.jda.entities.User User}.
     * Attempting to do so will result in nothing happening.
     *
     * @param user
     *          The {@link net.dv8tion.jda.entities.User User} that is having a {@link net.dv8tion.jda.entities.Role Role} removed.
     * @param roles
     *           The {@link net.dv8tion.jda.entities.Role Roles} that are being removed from the {@link net.dv8tion.jda.entities.User User}.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void removeRoleFromUser(User user, Role... roles)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.MANAGE_ROLES);

        Set<Role> addRoles = addedRoles.get(user);
        if (addRoles == null)
        {
            addRoles = new HashSet<>();
            addedRoles.put(user, addRoles);
        }
        Set<Role> removeRoles = removedRoles.get(user);
        if (removeRoles == null)
        {
            removeRoles = new HashSet<>();
            removedRoles.put(user, removeRoles);
        }
        for (Role role : roles)
        {
            if(guild.getPublicRole().equals(role))
                return;

            if (addRoles.contains(role))
                addRoles.remove(role);

            removeRoles.add(role);
        }
    }

    /**
     * This method will apply all accumulated changes received by setters
     *
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void update()
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }

        if (name != null || region != null || timeout != null || icon != null || !StringUtils.equals(afkChannelId, guild.getAfkChannelId()))
        {
            checkPermission(Permission.MANAGE_SERVER);

            JSONObject frame = getFrame();
            if(name != null)
                frame.put("name", name);
            if(region != null)
                frame.put("region", region.getKey());
            if(timeout != null)
                frame.put("afk_timeout", timeout.getSeconds());
            if(icon != null)
                frame.put("icon", icon == AvatarUtil.DELETE_AVATAR ? JSONObject.NULL : icon.getEncoded());
            if(!StringUtils.equals(afkChannelId, guild.getAfkChannelId()))
                frame.put("afk_channel_id", afkChannelId == null ? JSONObject.NULL : afkChannelId);
            update(frame);
        }

        if (addedRoles.size() > 0)
        {
            checkPermission(Permission.MANAGE_ROLES);

            for (User user : addedRoles.keySet())
            {
                List<Role> roles = guild.getRolesForUser(user);
                List<String> roleIds = new LinkedList<>();
                roles.forEach(r -> roleIds.add(r.getId()));

                addedRoles.get(user).stream().filter(role -> !roleIds.contains(role.getId())).forEach(role -> roleIds.add(role.getId()));
                removedRoles.get(user).stream().filter(role -> roleIds.contains(role.getId())).forEach(role -> roleIds.remove(role.getId()));

                ((JDAImpl) guild.getJDA()).getRequester().patch(
                        "https://discordapp.com/api/guilds/" + guild.getId() + "/members/" + user.getId(),
                        new JSONObject().put("roles", roleIds));

            }
            addedRoles.clear();
            removedRoles.clear();
        }
    }

    /**
     * Kicks a {@link net.dv8tion.jda.entities.User User} from the {@link net.dv8tion.jda.entities.Guild Guild}.
     * This change will be applied immediately.<br>
     * <p>
     * <b>Note:</b> {@link net.dv8tion.jda.entities.Guild#getUsers()} will still contain the {@link net.dv8tion.jda.entities.User User}
     * until Discord sends the {@link net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent GuildMemberLeaveEvent}.
     *
     * @param user
     *          The {@link net.dv8tion.jda.entities.User User} to kick from the from the {@link net.dv8tion.jda.entities.Guild Guild}.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void kick(User user)
    {
        kick(user.getId());
    }

    /**
     * Kicks the {@link net.dv8tion.jda.entities.User User} specified by the userId from the from the {@link net.dv8tion.jda.entities.Guild Guild}.
     * This change will be applied immediately.
     * <p>
     * <b>Note:</b> {@link net.dv8tion.jda.entities.Guild#getUsers()} will still contain the {@link net.dv8tion.jda.entities.User User}
     * until Discord sends the {@link net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent GuildMemberLeaveEvent}.
     *
     * @param userId
     *          The id of the {@link net.dv8tion.jda.entities.User User} to kick from the from the {@link net.dv8tion.jda.entities.Guild Guild}.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void kick(String userId)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.KICK_MEMBERS);

        ((JDAImpl) guild.getJDA()).getRequester().delete("https://discordapp.com/api/guilds/"
                + guild.getId() + "/members/" + userId);
    }

    /**
     * Bans a {@link net.dv8tion.jda.entities.User User} and deletes messages sent by the user
     * based on the amount of delDays.<br>
     * If you wish to ban a user without deleting any messages, provide delDays with a value of 0.
     * This change will be applied immediately.
     * <p>
     * <b>Note:</b> {@link net.dv8tion.jda.entities.Guild#getUsers()} will still contain the {@link net.dv8tion.jda.entities.User User}
     * until Discord sends the {@link net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent GuildMemberLeaveEvent}.
     *
     * @param user
     *          The {@link net.dv8tion.jda.entities.User User} to ban.
     * @param delDays
     *          The history of messages, in days, that will be deleted.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void ban(User user, int delDays)
    {
        ban(user.getId(), delDays);
    }

    /**
     * Bans the {@link net.dv8tion.jda.entities.User User} specified by the userId nd deletes messages sent by the user
     * based on the amount of delDays.<br>
     * If you wish to ban a user without deleting any messages, provide delDays with a value of 0.
     * This change will be applied immediately.
     * <p>
     * <b>Note:</b> {@link net.dv8tion.jda.entities.Guild#getUsers()} will still contain the {@link net.dv8tion.jda.entities.User User}
     * until Discord sends the {@link net.dv8tion.jda.events.guild.member.GuildMemberLeaveEvent GuildMemberLeaveEvent}.
     *
     * @param userId
     *          The id of the {@link net.dv8tion.jda.entities.User User} to ban.
     * @param delDays
     *          The history of messages, in days, that will be deleted.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void ban(String userId, int delDays)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.BAN_MEMBERS);

        ((JDAImpl) guild.getJDA()).getRequester().put("https://discordapp.com/api/guilds/"
                + guild.getId() + "/bans/" + userId + (delDays > 0 ? "?delete-message-days=" + delDays : ""), new JSONObject());
    }

    /**
     * Gets an unmodifiable list of the currently banned {@link net.dv8tion.jda.entities.User Users}.<br>
     * If you wish to ban or unban a user, please use one of the ban or unban methods of this Manager
     *
     * @return
     *      unmodifiable list of currently banned Users
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public List<User> getBans()
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        List<User> bans = new LinkedList<>();
        JSONArray bannedArr = ((JDAImpl) guild.getJDA()).getRequester().getA("https://discordapp.com/api/guilds/" + guild.getId() + "/bans");
        for (int i = 0; i < bannedArr.length(); i++)
        {
            JSONObject userObj = bannedArr.getJSONObject(i).getJSONObject("user");
            User u = guild.getJDA().getUserById(userObj.getString("id"));
            if (u != null)
            {
                bans.add(u);
            }
            else
            {
                //Create user here, instead of using the EntityBuilder (don't want to add users to registry)
                bans.add(new UserImpl(userObj.getString("id"), ((JDAImpl) guild.getJDA()))
                        .setUserName(userObj.getString("username"))
                        .setDiscriminator(userObj.get("discriminator").toString())
                        .setAvatarId(userObj.isNull("avatar") ? null : userObj.getString("avatar")));
            }
        }
        return Collections.unmodifiableList(bans);
    }

    /**
     * Unbans the provided {@link net.dv8tion.jda.entities.User User} from the {@link net.dv8tion.jda.entities.Guild Guild}.
     * This change will be applied immediately.
     *
     * @param user
     *          The {@link net.dv8tion.jda.entities.User User} to unban.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void unBan(User user)
    {
        unBan(user.getId());
    }

    /**
     * Unbans the {@link net.dv8tion.jda.entities.User User} from the {@link net.dv8tion.jda.entities.Guild Guild} based on the provided userId.
     * This change will be applied immediately.
     *
     * @param userId
     *          The id of the {@link net.dv8tion.jda.entities.User User} to unban.
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void unBan(String userId)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        checkPermission(Permission.BAN_MEMBERS);

        ((JDAImpl) guild.getJDA()).getRequester().delete("https://discordapp.com/api/guilds/"
                + guild.getId() + "/bans/" + userId);
    }

    /**
     * Transfers the ownership of this Guild to another user.
     * This will only work, if the current owner of the Guild is the JDA-user.
     * This change will be applied immediately.
     *
     * @param newOwner
     *      the desired new Owner
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void transferOwnership(User newOwner)
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        if (!guild.getJDA().getSelfInfo().getId().equals(guild.getOwnerId()))
        {
            throw new PermissionException("Moving guild-ownership is only available for guild-owners!");
        }
        if (!guild.getUsers().contains(newOwner))
        {
            throw new IllegalArgumentException("The new owner is not member of the Guild!");
        }
        update(getFrame().put("owner_id", newOwner.getId()));
    }

    /**
     * Leaves or Deletes this {@link net.dv8tion.jda.entities.Guild Guild}.
     * If the logged in {@link net.dv8tion.jda.entities.User User} is the owner of
     * this {@link net.dv8tion.jda.entities.Guild Guild}, the {@link net.dv8tion.jda.entities.Guild Guild} is deleted.
     * Otherwise, this guild will be left.
     * This change will be applied immediately.
     *
     * @throws net.dv8tion.jda.exceptions.GuildUnavailableException
     *      if the guild is temporarily unavailable
     */
    public void leaveOrDelete()
    {
        if (!guild.isAvailable())
        {
            throw new GuildUnavailableException();
        }
        ((JDAImpl) guild.getJDA()).getRequester().delete("https://discordapp.com/api/guilds/" + guild.getId());
    }

    private JSONObject getFrame()
    {
        return new JSONObject().put("name", guild.getName());
    }

    private void update(JSONObject object)
    {
        ((JDAImpl) guild.getJDA()).getRequester().patch("https://discordapp.com/api/guilds/" + guild.getId(), object);
    }

    private void checkPermission(Permission perm)
    {
        if (!PermissionUtil.checkPermission(getGuild().getJDA().getSelfInfo(), perm, getGuild()))
            throw new PermissionException(perm);

    }
}