#pragma once
#include <QMainWindow>
#include <QSystemTrayIcon>
#include <QMenu>

class MainWindow : public QMainWindow {
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);

private slots:
    void onTrayActivated(QSystemTrayIcon::ActivationReason reason);
    void onQuitClicked();

private:
    QSystemTrayIcon *trayIcon;
    QMenu *trayMenu;
};
