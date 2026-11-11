"use strict";
/**
 * HBX SOLAR ONE — Trusted Backend Types & Security Enums
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.AccountStatus = exports.UserRole = void 0;
var UserRole;
(function (UserRole) {
    UserRole["SUPER_ADMIN"] = "SUPER_ADMIN";
    UserRole["ADMIN"] = "ADMIN";
    UserRole["OPERATIONS_MANAGER"] = "OPERATIONS_MANAGER";
    UserRole["SALES_MANAGER"] = "SALES_MANAGER";
    UserRole["TEAM_LEADER"] = "TEAM_LEADER";
    UserRole["BDE"] = "BDE";
    UserRole["INSTALLATION_ENGINEER"] = "INSTALLATION_ENGINEER";
    UserRole["SERVICE_ENGINEER"] = "SERVICE_ENGINEER";
    UserRole["PARTNER"] = "PARTNER";
    UserRole["CUSTOMER"] = "CUSTOMER";
})(UserRole || (exports.UserRole = UserRole = {}));
var AccountStatus;
(function (AccountStatus) {
    AccountStatus["ACTIVE"] = "ACTIVE";
    AccountStatus["PENDING_APPROVAL"] = "PENDING_APPROVAL";
    AccountStatus["SUSPENDED"] = "SUSPENDED";
    AccountStatus["INACTIVE"] = "INACTIVE";
})(AccountStatus || (exports.AccountStatus = AccountStatus = {}));
//# sourceMappingURL=types.js.map