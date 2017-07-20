package org.exoplatform.social.injector;

import org.apache.commons.io.IOUtils;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.injector.module.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.picocontainer.Startable;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
/**
 * Created by kmenzli on 7/20/17.
 */
public class DataInjector implements Startable {
    private static final Log LOG= ExoLogger.getLogger(DataInjector.class);


    protected PortalContainer portalContainer_;


    /** The scenario folder. */
    public String                   SCENARIOS_FOLDER                = "META-INF/scenarios";

    /** The scenario name attribute. */
    public String                   SCENARIO_NAME_ATTRIBUTE        = "scenarioName";

    /** The scenarios. */
    private Map<String, JSONObject> scenarios;

    UserModule userModule_;

    /** The space service. */
    SpaceModule spaceModule_;

    /** The calendar service. */
    CalendarModule calendarModule_;

    /** The wiki service. */
    WikiModule wikiModule_;

    /** The forum service. */
    ForumModule forumModule_;

    /** The document service. */
    DocumentModule documentModule_;

    /** The activity service. */
    ActivityModule activityModule_;

    public DataInjector(PortalContainer portalContainer, UserModule userModule, SpaceModule spaceModule, CalendarModule calendarModule, WikiModule wikiModule, ForumModule forumModule, DocumentModule documentModule, ActivityModule activityModule  ) {

        //--- Init Services
        userModule_ = userModule;
        spaceModule_ =spaceModule;
        calendarModule_ = calendarModule;
        wikiModule_ = wikiModule;
        forumModule_ = forumModule;
        documentModule_ = documentModule;
        activityModule_ = activityModule;

        portalContainer_ = portalContainer;

        scenarios = new HashMap<String, JSONObject>();
        try {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(SCENARIOS_FOLDER + "/" + "community_fake_data_level_a.json");
            String fileContent = getData(stream);
            try {
                JSONObject json = new JSONObject(fileContent);
                String name = json.getString(SCENARIO_NAME_ATTRIBUTE);
                scenarios.put(name, json);
            } catch (JSONException e) {
                LOG.error("Syntax error in scenario ", e);
            }

        } catch (Exception e) {
            LOG.error("Unable to read scenario file", e);
        }


    }

    @Override
    public void start() {
        LOG.info("Start {} .............", this.getClass().getName());

        PortalContainer.addInitTask(portalContainer_.getPortalContext(), new RootContainer.PortalContainerPostInitTask(){
            @Override
            public void execute(ServletContext context, PortalContainer portalContainer)
            {
                // Execute the load resources in an asynchronous way
                new Thread(new Runnable(){
                    @Override
                    public void run(){

                        //--- Drop all injected data
                        //--- Start data injection
                        String downloadUrl = "";
                        try {
                            JSONObject scenarioData = scenarios.get("Tribe_Fake_Data_Level_A").getJSONObject("data");
                            if (scenarioData.has("users")) {
                                LOG.info("Create " + scenarioData.getJSONArray("users").length() + " users.");
                                userModule_.createUsers(scenarioData.getJSONArray("users"));

                            }
                            if (scenarioData.has("relations")) {
                                LOG.info("Create " + scenarioData.getJSONArray("relations").length() + " relations.");
                                userModule_.createRelations(scenarioData.getJSONArray("relations"));
                            }
                            if (scenarioData.has("spaces")) {
                                LOG.info("Create " + scenarioData.getJSONArray("spaces").length() + " spaces.");
                                spaceModule_.createSpaces(scenarioData.getJSONArray("spaces"));
                            }

                        } catch (JSONException e) {
                            LOG.error("Syntax error when reading scenario " , e);
                        }

                    }
                },"TribeInjectorProcess").start();
            }
        });

    }

    @Override
    public void stop() {

    }


    /**
     * Gets the data.
     *
     * @param inputStream the input stream
     * @return the data
     */
    public String getData(InputStream inputStream) {
        String out = "";
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(inputStream, writer);
            out = writer.toString();

        } catch (IOException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }

        return out;
    }
}
