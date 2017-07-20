package org.exoplatform.social.injector.module;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.chromattic.ChromatticManager;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.injector.InjectorUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kmenzli on 7/20/17.
 */
public class SpaceModule {
    /** The log. */
    private final Log LOG = ExoLogger.getLogger(SpaceModule.class);

    /**
     * Instantiates a new space service.
     */
    public SpaceModule() {
    }

    /**
     * Creates the spaces.
     *
     * @param spaces the spaces
     */
    public void createSpaces(JSONArray spaces) {
        for (int i = 0; i < spaces.length(); i++) {

            try {
                JSONObject space = spaces.getJSONObject(i);
                createSpace(space.getString("displayName"), space.getString("creator"));
                if (space.has("members")) {
                    JSONArray members = space.getJSONArray("members");
                    for (int j = 0; j < members.length(); j++) {
                        Space spacet = CommonsUtils.getService(SpaceService.class).getSpaceByDisplayName(space.getString("displayName"));
                        if (spacet != null) {
                            CommonsUtils.getService(SpaceService.class).addMember(spacet, members.getString(j));
                        }

                    }
                }
                createSpaceAvatar(space.getString("displayName"), space.getString("creator"), space.getString("avatar"));

            } catch (JSONException e) {
                LOG.error("Syntax error on space n°" + i, e);
            }
        }
    }

    /**
     * Creates the space avatar.
     *
     * @param name the name
     * @param editor the editor
     * @param avatarFile the avatar file
     */
    private void createSpaceAvatar(String name, String editor, String avatarFile) {
        Space space = null;
        try {
            space = CommonsUtils.getService(SpaceService.class).getSpaceByDisplayName(name);
            if (space != null) {
                try {
                    AvatarAttachment avatarAttachment = InjectorUtils.getAvatarAttachment(avatarFile);
                    space.setAvatarAttachment(avatarAttachment);
                    CommonsUtils.getService(SpaceService.class).updateSpace(space);
                    space.setEditor(editor);
                    CommonsUtils.getService(SpaceService.class).updateSpaceAvatar(space);
                } catch (Exception e) {
                    LOG.error("Unable to set avatar for space " + space.getDisplayName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            LOG.error("Unable to create space " + space.getDisplayName(), e.getMessage());
        }

    }

    /**
     * Creates the space.
     *
     * @param name the name
     * @param creator the creator
     */
    private void createSpace(String name, String creator) {
        EntityManagerService entityManagerService = CommonsUtils.getService(EntityManagerService.class);
        ChromatticManager chromatticManager = CommonsUtils.getService(ChromatticManager.class);
        Space target = null;
        try {
            RequestLifeCycle.begin(entityManagerService);
            target = CommonsUtils.getService(SpaceService.class).getSpaceByDisplayName(name);

            if (target != null) {
                return;
            }

            Space space = new Space();
            // space.setId(name);
            space.setDisplayName(name);
            space.setPrettyName(name);
            space.setDescription(StringUtils.EMPTY);
            space.setGroupId("/spaces/" + space.getPrettyName());
            space.setRegistration(Space.OPEN);
            space.setVisibility(Space.PRIVATE);
            space.setPriority(Space.INTERMEDIATE_PRIORITY);

            Identity identity = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), true);
            if (identity != null) {
                space.setPrettyName(SpaceUtils.buildPrettyName(space));
            }
            space.setType(DefaultSpaceApplicationHandler.NAME);
            RequestLifeCycle.begin(chromatticManager);
            CommonsUtils.getService(SpaceService.class).createSpace(space, creator);
        } catch (Exception E) {
            LOG.error( "========= ERROR when create space {} ",target.getPrettyName(),E);

        } finally {
            RequestLifeCycle.end();

        }

    }
}
