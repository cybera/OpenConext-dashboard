import React from "react";
import I18n from "i18n-js";

import LicenseInfo from "./license_info";
import { AppShape } from "../shapes";

class LicenseInfoPanel extends React.Component {
  render() {
    return (
      <div className="l-middle">
        <div className="mod-title">
          <h1>{I18n.t("license_info_panel.title")}</h1>
        </div>

        <div className="mod-connection">
          <LicenseInfo app={this.props.app} split={false} />
        </div>
        {this.renderLicenseRequired()}
      </div>
    );
  }

  renderLicenseRequired() {
    switch (this.props.app.licenseStatus) {
    case true:
      return null;
    case false:
      return null;
    default:
      return null;
    }
  }

  renderHasLicense(msg) {
    return (
      <div className="mod-title">
        <h3 dangerouslySetInnerHTML={{ __html: msg }} />
      </div>
    );
  }

  renderNoLicense() {
    return (
      <div className="mod-title">
        <h3 dangerouslySetInnerHTML={{ __html: I18n.t("license_info_panel.no_license_html") }} />
        <br />
        <div className="mod-description" dangerouslySetInnerHTML={{ __html: I18n.t("license_info_panel.no_license_description_html") }} />
      </div>
    );
  }

  renderNoLicenseNeeded() {
    return (
      <div className="mod-title">
        <h3 dangerouslySetInnerHTML={{ __html: I18n.t("license_info_panel.not_needed_html") }} />
      </div>
    );
  }

  renderUnknownLicense() {
    return (
      <div className="mod-title">
        <h3>{I18n.t("license_info_panel.unknown_license")}</h3>
        {/*<br />*/}
        {/*<div className="mod-description" dangerouslySetInnerHTML={{ __html: I18n.t("license_info_panel.unknown_license_description_html") }} />*/}
      </div>
    );
  }
}

LicenseInfoPanel.propTypes = {
  app: AppShape.isRequired
};

export default LicenseInfoPanel;
