// -----------------------------------------------------------
//
// Dashboard
//
// Manage the dashboard. Root component
//
// -----------------------------------------------------------

import React from 'react';

import {Button, InlineNotification, Tag} from "carbon-components-react";
import {Card} from 'react-bootstrap';
import Chart from "../component/Chart";
import RestCallService from "../services/RestCallService";
import {ArrowRepeat} from "react-bootstrap-icons";


class Dashboard extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            dashboard: {
                cluster: {
                    clusterSize: "",
                    partitionsCount: "",
                    replicationfactor: "",
                    statusCluster: "ACTIVE"
                },
                backup: {
                    history: [],
                    statusBackup: "",
                    step: "2/7 backup Operate",
                    backups: ""
                },
                configuration: {
                    statusConfiguration: "CORRECT",
                },
                scheduler: {
                    statusScheduler: "INACTIF",
                    cron: "",
                    next: "",
                    delay: ""
                }
            },
            display: {
                loading: true
            },


        };
        this.schedule = this.schedule.bind(this);
        this.setDisplayProperty= this.setDisplayProperty.bind(this);
    }

    componentDidMount() {
        this.refreshDashboard();


        // Set up the interval to call schedule() every 30 seconds
        this.intervalId = setInterval(this.schedule, 120000);
    }
    // Cleanup to clear the interval when the component unmounts
    componentWillUnmount() {
        clearInterval(this.intervalId);
    }

    render() {
        // console.log("dashboard.render display="+JSON.stringify(this.state.display));
        return (<div className={"container"}>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-10">
                        <h1 className="title">Dashboard</h1>
                        <InlineNotification kind="info" hideCloseButton="true" lowContrast="false">
                            Give the last backup time and status, if a next backup is scheduled.
                        </InlineNotification>
                    </div>

                    <div className="col-md-2">
                        <Button className="btn btn-success btn-sm"
                                onClick={() => {
                                    this.refreshDashboard()
                                }}
                                disabled={this.state.display.loading}>
                            <ArrowRepeat/> Refresh
                        </Button>
                    </div>

                </div>

                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-12">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Backup timeline</Card.Header>
                            <Card.Body>
                                <Chart type="HorizontalBar" dataList={this.state.dashboard.backup.history}
                                       oneColor={true}
                                       options={{
                                           title: this.state.title,
                                           showXLabel: false,
                                           showYLabel: true,
                                           width: 200,
                                           height: 100,
                                           showGrid: false
                                       }}
                                       title="Backup timeline"/>

                            </Card.Body>
                        </Card>
                    </div>
                </div>
                <div className="row" style={{width: "100%", marginTop: "10px"}}>
                    <div className="col-md-4">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Backup</Card.Header>
                            <Card.Body>
                                <table style={{borderCollapse: "separate", borderSpacing: "10px"}}>
                                    <tr>
                                        <td style={{verticalAlign: "middle"}}>Status</td>
                                        <td style={{textAlign: "center"}}>
                                            {this.state.dashboard.backup.statusBackup === "INPROGRESS" &&
                                                <div>
                                                    <Tag type="blue">In progress</Tag><br/>
                                                    {this.state.dashboard.backup.step}
                                                </div>
                                            }
                                            {this.state.dashboard.backup.statusBackup === "FAILED" &&
                                                <div>
                                                    <Tag type="red">Failed</Tag><br/>
                                                    {this.state.dashboard.backup.step}
                                                </div>
                                            }
                                            {this.state.dashboard.backup.statusBackup === "" &&
                                                <Tag type="green">Ready</Tag>}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Total number of backups</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.backup.backupsCount}</td>
                                    </tr>
                                    <tr>
                                        <td>backup complete</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.backup.backupsComplete}</td>
                                    </tr>
                                    <tr>
                                        <td>backup failed</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.backup.backupsFailed}</td>
                                    </tr>
                                    <tr>
                                        <td>Next execution</td>
                                        <td>
                                            {this.state.dashboard.scheduler.next}<br/>
                                            in&nbsp;
                                            {this.state.dashboard.scheduler.delay}
                                        </td>
                                    </tr>
                                </table>
                            </Card.Body>
                        </Card>
                    </div>
                    <div className="col-md-4">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Configuration</Card.Header>
                            <Card.Body>
                                <table style={{borderCollapse: "separate", borderSpacing: "10px"}}>
                                    <tr>
                                        <td>Status</td>
                                        <td style={{textAlign: "center"}}>
                                            {this.state.dashboard.configuration.statusConfiguration === "CORRECT" &&
                                                <Tag type="green">Correct</Tag>}
                                            {this.state.dashboard.configuration.statusConfiguration !== "CORRECT" &&
                                                <Tag type="red">Failed</Tag>}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Scheduler</td>
                                        <td style={{textAlign: "center"}}>
                                            {this.state.dashboard.scheduler.statusScheduler === "ACTIVE" &&
                                                <Tag type="green">Active</Tag>}
                                            {this.state.dashboard.scheduler.statusScheduler !== "ACTIVE" &&
                                                <Tag type="gray">Inactive</Tag>}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Scheduler Cron</td>
                                        <td>
                                            {this.state.dashboard.scheduler.cron}
                                        </td>
                                    </tr>
                                </table>
                            </Card.Body>
                        </Card>
                    </div>
                    <div className="col-md-4">
                        <Card>
                            <Card.Header style={{backgroundColor: "rgba(0,0,0,.03)"}}>Cluster
                                parameters</Card.Header>
                            <Card.Body>
                                <table style={{borderCollapse: "separate", borderSpacing: "10px"}}>
                                    <tr>
                                        <td style={{verticalAlign: "middle"}}>Status</td>
                                        <td style={{textAlign: "center"}}>
                                            {this.state.dashboard.cluster.statusCluster === "ACTIF" &&
                                                <Tag type="green">Started</Tag>}
                                            {this.state.dashboard.cluster.statusCluster !== "ACTIF" &&
                                                <Tag type="gray">Pause</Tag>}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>Cluster Size</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.cluster.clusterSize}</td>
                                    </tr>
                                    <tr>
                                        <td>Partitions</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.cluster.partitionsCount}</td>
                                    </tr>
                                    <tr>
                                        <td>Replication Factor</td>
                                        <td style={{textAlign: "right"}}>{this.state.dashboard.cluster.replicationfactor}</td>
                                    </tr>
                                </table>
                            </Card.Body>
                        </Card>
                    </div>
                </div>
            </div>
        )

    }


    refreshDashboard = () => {
        let uri = 'blueberry/api/dashboard/all?forceRefresh=true';
        console.log("DashBoard.refreshDashboard http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshDashboardCallback);
    }

    refreshDashboardCallback= (httpPayload) => {
        console.log("DashBoard.refreshDashboardCallback");

        this.setDisplayProperty("loading", false);
        if (httpPayload.isError()) {
            console.log("Dashboard.refreshDashboardCallback: error " + httpPayload.getError());
            this.setState({status: "Error"});
        } else {
            this.setState({dashboard: httpPayload.getData()});

        }
    }

    /**
     * Set the display property
     * @param propertyName name of the property
     * @param propertyValue the value
     */
    setDisplayProperty= (propertyName, propertyValue) => {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

    schedule () {
        let uri = 'blueberry/api/dashboard/all?forceRefresh=false';
        console.log("DashBoard.schedule Schedule http[" + uri + "]");

        this.setDisplayProperty("loading", true);
        this.setState({status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson(uri, this, this.refreshDashboardCallback);


    }
}

export default Dashboard;
