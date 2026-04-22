#include "mainwindow.h"
#include <QApplication>
#include <QIcon>

MainWindow::MainWindow(QWidget *parent) : QMainWindow(parent) {
    // Create tray menu
    trayMenu = new QMenu(this);
    trayMenu->addAction("Open", this, &MainWindow::show);
    trayMenu->addSeparator();
    trayMenu->addAction("Quit", this, &MainWindow::onQuitClicked);

    // Create tray icon
    trayIcon = new QSystemTrayIcon(this);
    trayIcon->setIcon(QIcon::fromTheme("folder-open")); // system icon for now
    trayIcon->setContextMenu(trayMenu);
    trayIcon->setToolTip("DropIn — linked to no device");
    trayIcon->show();

    // Connect signal to slot — like @EventListener wired to an event publisher
    connect(trayIcon, &QSystemTrayIcon::activated,
            this, &MainWindow::onTrayActivated);
}

void MainWindow::onTrayActivated(QSystemTrayIcon::ActivationReason reason) {
    if (reason == QSystemTrayIcon::Trigger) {
        isVisible() ? hide() : show();
    }
}

void MainWindow::onQuitClicked() {
    QApplication::quit();
}