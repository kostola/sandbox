package com.redhat.service.smartevents.processor.actions.ansibletower;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AnsibleTowerAction extends GatewayBean {

    String TYPE = "AnsibleTower";
    String HOST_PARAM = "host";
    String USERNAME_PARAM = "username";
    String PASSWORD_PARAM = "password";
    String JOB_TEMPLATE_ID_PARAM = "job_template_id";

    @Override
    default String getType() {
        return TYPE;
    }
}
