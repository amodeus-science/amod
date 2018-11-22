clear all; close all; clc;

load Requets200000;
timestep = 5;

numberRequests = Request.numberRequests;
time5min = zeros(length(numberRequests),1);

for i=1:1:length(numberRequests)
        time5min(i) = i*timestep/(60);
end

totalRequests = sum(numberRequests);
stringTotalRequest = sprintf('Total %d Requests',totalRequests);

plot(time5min, numberRequests, 'b', 'LineWidth', 1);
ylabel('Requests');
xlabel('Time of Day [h]');
xlim([0 24]);
grid on;
legend(stringTotalRequest,'Location', 'NE');
%     allTextHandles = findall(gca, 'Type', 'text');
%     set([gca; allTextHandles], 'FontName', 'Arial', 'FontSize', 18);
allTextHandles = findall(gca, 'Type', 'text');
set([gca; allTextHandles], 'FontName', 'Times New Roman', 'FontSize', 12);
fig = gcf;
fig.PaperPositionMode = 'auto'
fig_pos = fig.PaperPosition;
fig.PaperSize = [fig_pos(3) fig_pos(4)];
print(fig,'requestTotal','-dpdf')