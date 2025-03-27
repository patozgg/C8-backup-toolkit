// -----------------------------------------------------------
//
// Parameters
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import RestCallService from "../services/RestCallService";
import {Button, InlineNotification} from "carbon-components-react";
import {ArrowRepeat} from "react-bootstrap-icons";


class Restore extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            secrets: [],
            display: {loading: false}
        };
    }

    componentDidMount(prevProps) {
        this.refreshListBackup();
    }

    render() {
        return (
            <div className={"container"}>
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Restore</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Select a backup, and start the restoration. All current value will be lost, and during the restoration, the platform is not accessible.
                        </InlineNotification>

                    </div>
                </div>
                <div className="row" style={{marginTop: "10px"}}>
                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => {
                                    this.refreshList()
                                }}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh List Backup
                        </Button>
                    </div>
                </div>

                <table id="runnersTable" className="table is-hoverable is-fullwidth">
                    <thead>
                    <tr>
                        <th>Backup ID</th>
                        <th>Date</th>
                        <th>Status</th>
                    </tr>
                    </thead>
                    <tbody>
                    </tbody>
                </table>



            </div>
        )
    }

    getStyleRow(secret) {
        return {};
    }


    refreshListBackup() {
        let uri = 'blueberry/api/checkup.';
        console.log("checkup.checkup http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshListBackupCallback);
    }

    refreshListBackupCallback(httpPayload) {
    }
    /**
     * Set the display property
     * @param propertyName name of the property
     * @param propertyValue the value
     */
    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }
}

export default Restore;