import React from "react";
import I18n from "i18n-js";
import { AppShape } from "../shapes";

class LicenseInfo extends React.Component {
  render() {
    switch (this.props.app.licenseRequired) {
    case true:
      return this.renderLicenseRequired(this.props.app);
    case false:
      return this.renderNoLicenseRequired();
    default:
      return null;
    }
  }

  renderSplitClass(classNames) {
    if (this.props.split) {
      return classNames + " split";
    }

    return classNames;
  }

  renderLicenseRequired(app) {
    const licenseRequired = I18n.t("license_info.license_needed");
    const licenseDetails = app.licenseDetails.en;
    return (
      <div className={this.renderSplitClass("license yes")}>
        <i className="fa fa-file-text-o"></i>
        <h2>{licenseRequired}</h2>
        <p>{licenseDetails}</p>
      </div>
    );
  }

  renderNoLicenseRequired() {
    return (
      <div className={this.renderSplitClass("license no-needed")}>
        <i className="fa fa-file-text-o"></i>
        <h2>{I18n.t("license_info.no_license_needed")}</h2>
      </div>
    );
  }
}

LicenseInfo.defaultProps = {
  showLinks: false,
  split: true
};

LicenseInfo.propTypes = {
  app: AppShape.isRequired,
  showLinks: React.PropTypes.bool,
  split: React.PropTypes.bool
};

export default LicenseInfo;
